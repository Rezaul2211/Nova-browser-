package com.example.privacy

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class FilterEngine {

    private val _cumulativeStats = MutableStateFlow(CumulativePrivacyStats())
    val cumulativeStats: StateFlow<CumulativePrivacyStats> = _cumulativeStats.asStateFlow()

    // Per-tab or per-page stats stored by pageUrl or tabId
    private val pageStatsMap = ConcurrentHashMap<String, PagePrivacyStats>()

    // Set of allowed domains where user explicitly turned off ad blocking
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()

    fun setAllowedDomains(domains: Set<String>) {
        allowedDomains.clear()
        allowedDomains.addAll(domains.map { it.lowercase() })
    }

    fun isDomainAllowed(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val cleanHost = host.lowercase()
        return allowedDomains.contains(cleanHost) || allowedDomains.any { cleanHost.endsWith(".$it") }
    }

    data class FilterResult(
        val shouldBlock: Boolean,
        val reason: BlockReason? = null,
        val isThirdParty: Boolean = false,
        val host: String = ""
    )

    /**
     * Inspects an outgoing web resource request and determines if it should be blocked.
     */
    fun shouldBlockRequest(
        requestUrl: Uri,
        pageUrl: Uri?,
        adBlockingEnabled: Boolean,
        trackerBlockingEnabled: Boolean
    ): FilterResult {
        val requestHost = requestUrl.host?.lowercase() ?: return FilterResult(false)
        val pageHost = pageUrl?.host?.lowercase() ?: ""

        // If site is allowlisted by user, do not block
        if (isDomainAllowed(pageHost)) {
            return FilterResult(false, host = requestHost)
        }

        val isThirdParty = pageHost.isNotBlank() && !isSameOriginOrSubdomain(pageHost, requestHost)

        // 1. Check known Ad Networks
        if (adBlockingEnabled && isAdHost(requestHost)) {
            return FilterResult(
                shouldBlock = true,
                reason = BlockReason.AD_NETWORK,
                isThirdParty = isThirdParty,
                host = requestHost
            )
        }

        // 2. Check known Trackers & Analytics
        if (trackerBlockingEnabled && isTrackerHost(requestHost)) {
            return FilterResult(
                shouldBlock = true,
                reason = BlockReason.TRACKER,
                isThirdParty = isThirdParty,
                host = requestHost
            )
        }

        // 3. Check URL path patterns
        val fullUrlStr = requestUrl.toString()
        if (adBlockingEnabled || trackerBlockingEnabled) {
            for (pattern in FilterRules.AD_PATH_PATTERNS) {
                if (pattern.matches(fullUrlStr)) {
                    val reason = if (isThirdParty) BlockReason.THIRD_PARTY_RESTRICTION else BlockReason.AD_NETWORK
                    return FilterResult(
                        shouldBlock = true,
                        reason = reason,
                        isThirdParty = isThirdParty,
                        host = requestHost
                    )
                }
            }
        }

        return FilterResult(
            shouldBlock = false,
            isThirdParty = isThirdParty,
            host = requestHost
        )
    }

    fun recordRequestEvent(
        tabId: String,
        pageUrl: String,
        requestUrl: String,
        result: FilterResult
    ): PagePrivacyStats {
        val pageHost = try { Uri.parse(pageUrl).host ?: "" } catch (e: Exception) { "" }
        val hasHttps = pageUrl.startsWith("https://", ignoreCase = true)

        val current = pageStatsMap[tabId] ?: PagePrivacyStats(
            pageUrl = pageUrl,
            pageHost = pageHost,
            isShieldActive = !isDomainAllowed(pageHost),
            hasHttps = hasHttps
        )

        val updated = if (result.shouldBlock && result.reason != null) {
            val blockedInfo = BlockedRequestInfo(
                url = requestUrl,
                host = result.host,
                reason = result.reason
            )
            val adsInc = if (result.reason == BlockReason.AD_NETWORK || result.reason == BlockReason.THIRD_PARTY_RESTRICTION) 1 else 0
            val trackerInc = if (result.reason == BlockReason.TRACKER || result.reason == BlockReason.FINGERPRINTING) 1 else 0
            val thirdPartyInc = if (result.isThirdParty) 1 else 0

            // Update cumulative
            _cumulativeStats.update {
                it.copy(
                    totalAdsBlocked = it.totalAdsBlocked + adsInc,
                    totalTrackersBlocked = it.totalTrackersBlocked + trackerInc,
                    totalThirdPartyBlocked = it.totalThirdPartyBlocked + thirdPartyInc,
                    totalRequestsIntercepted = it.totalRequestsIntercepted + 1
                )
            }

            current.copy(
                pageUrl = pageUrl,
                pageHost = pageHost,
                totalRequests = current.totalRequests + 1,
                adsBlocked = current.adsBlocked + adsInc,
                trackersBlocked = current.trackersBlocked + trackerInc,
                thirdPartyBlocked = current.thirdPartyBlocked + thirdPartyInc,
                blockedDomains = (listOf(blockedInfo) + current.blockedDomains).take(50),
                isShieldActive = !isDomainAllowed(pageHost),
                hasHttps = hasHttps
            )
        } else {
            _cumulativeStats.update {
                it.copy(totalRequestsIntercepted = it.totalRequestsIntercepted + 1)
            }
            current.copy(
                pageUrl = pageUrl,
                pageHost = pageHost,
                totalRequests = current.totalRequests + 1,
                isShieldActive = !isDomainAllowed(pageHost),
                hasHttps = hasHttps
            )
        }

        pageStatsMap[tabId] = updated
        return updated
    }

    fun resetTabStats(tabId: String, newUrl: String) {
        val host = try { Uri.parse(newUrl).host ?: "" } catch (e: Exception) { "" }
        val hasHttps = newUrl.startsWith("https://", ignoreCase = true)
        pageStatsMap[tabId] = PagePrivacyStats(
            pageUrl = newUrl,
            pageHost = host,
            isShieldActive = !isDomainAllowed(host),
            hasHttps = hasHttps
        )
    }

    fun getPageStats(tabId: String): PagePrivacyStats {
        return pageStatsMap[tabId] ?: PagePrivacyStats()
    }

    private fun isAdHost(host: String): Boolean {
        if (FilterRules.AD_HOSTS.contains(host)) return true
        return FilterRules.AD_HOSTS.any { host.endsWith(".$it") }
    }

    private fun isTrackerHost(host: String): Boolean {
        if (FilterRules.TRACKER_HOSTS.contains(host)) return true
        return FilterRules.TRACKER_HOSTS.any { host.endsWith(".$it") }
    }

    private fun isSameOriginOrSubdomain(baseHost: String, requestHost: String): Boolean {
        if (baseHost == requestHost) return true
        if (requestHost.endsWith(".$baseHost")) return true
        val baseDomain = getRegisteredDomain(baseHost)
        val requestDomain = getRegisteredDomain(requestHost)
        return baseDomain.isNotBlank() && baseDomain == requestDomain
    }

    private fun getRegisteredDomain(host: String): String {
        val parts = host.split(".")
        return if (parts.size >= 2) {
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            host
        }
    }
}
