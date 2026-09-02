package com.example.privacy

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Ad Blocking & Privacy Engine.
 * Integrates Request Classification, Video Ad Shielding, Anti-Redirect Protection,
 * Anti-Anti-Adblock handling, and Dynamic Cosmetic Filtering.
 */
class AdBlockEngine {

    val ruleManager = FilterRuleManager()
    val requestInterceptor = NetworkRequestInterceptor(ruleManager)
    val siteExceptionManager = SiteExceptionManager()
    val navigationProtectionEngine = NavigationProtectionEngine(this)

    // Dedicated Video-Ad Filtering Layer
    val videoAdRuleManager = VideoAdRuleManager()
    val mediaRequestClassifier = MediaRequestClassifier(videoAdRuleManager)
    val vastVmapDetector = VastVmapDetector(videoAdRuleManager)
    val manifestInspector = ManifestInspector(videoAdRuleManager)
    val videoAdDetector = VideoAdDetector(videoAdRuleManager, mediaRequestClassifier, vastVmapDetector)
    val videoAdRequestInterceptor = VideoAdRequestInterceptor(videoAdDetector, vastVmapDetector, manifestInspector)
    val adNavigationGuard = AdNavigationGuard { siteExceptionManager.isDomainAllowed(it) }

    private val _cumulativeStats = MutableStateFlow(CumulativePrivacyStats())
    val cumulativeStats: StateFlow<CumulativePrivacyStats> = _cumulativeStats.asStateFlow()

    private val pageStatsMap = ConcurrentHashMap<String, PagePrivacyStats>()

    fun setAllowedDomains(domains: Set<String>) {
        siteExceptionManager.setAllowedDomains(domains)
    }

    fun isDomainAllowed(host: String?): Boolean {
        return siteExceptionManager.isDomainAllowed(host)
    }

    fun evaluateNetworkRequest(
        requestUrl: Uri,
        pageUrl: Uri?,
        adBlockingEnabled: Boolean,
        trackerBlockingEnabled: Boolean,
        videoAdProtectionEnabled: Boolean,
        headers: Map<String, String>? = null
    ): RequestEvaluation {
        // Evaluate through core network interceptor
        val eval = requestInterceptor.evaluateRequest(
            requestUrl = requestUrl,
            pageUrl = pageUrl,
            adBlockingEnabled = adBlockingEnabled,
            trackerBlockingEnabled = trackerBlockingEnabled,
            videoAdProtectionEnabled = videoAdProtectionEnabled,
            headers = headers,
            isDomainAllowed = { siteExceptionManager.isDomainAllowed(it) }
        )

        if (eval.shouldBlock) return eval

        // If video ad protection is active, run secondary deep inspection
        if (videoAdProtectionEnabled) {
            val videoEval = videoAdDetector.evaluate(requestUrl, pageUrl, headers)
            if (videoEval.isVideoAd) {
                return RequestEvaluation(
                    shouldBlock = true,
                    reason = BlockReason.VIDEO_AD,
                    isThirdParty = eval.isThirdParty,
                    host = requestUrl.host ?: "",
                    resourceType = ResourceType.MEDIA
                )
            }
        }

        return eval
    }

    fun recordRequestEvent(
        tabId: String,
        pageUrl: String,
        requestUrl: String,
        evaluation: RequestEvaluation
    ): PagePrivacyStats {
        val pageHost = try { Uri.parse(pageUrl).host ?: "" } catch (e: Exception) { "" }
        val hasHttps = pageUrl.startsWith("https://", ignoreCase = true)

        val current = pageStatsMap[tabId] ?: PagePrivacyStats(
            pageUrl = pageUrl,
            pageHost = pageHost,
            isShieldActive = !isDomainAllowed(pageHost),
            hasHttps = hasHttps
        )

        val updated = if (evaluation.shouldBlock && evaluation.reason != null) {
            val blockedInfo = BlockedRequestInfo(
                url = requestUrl,
                host = evaluation.host,
                reason = evaluation.reason
            )
            val adsInc = if (evaluation.reason == BlockReason.AD_NETWORK || evaluation.reason == BlockReason.THIRD_PARTY_RESTRICTION) 1 else 0
            val trackerInc = if (evaluation.reason == BlockReason.TRACKER || evaluation.reason == BlockReason.FINGERPRINTING) 1 else 0
            val videoAdInc = if (evaluation.reason == BlockReason.VIDEO_AD) 1 else 0
            val thirdPartyInc = if (evaluation.isThirdParty) 1 else 0

            _cumulativeStats.update {
                it.copy(
                    totalAdsBlocked = it.totalAdsBlocked + adsInc,
                    totalTrackersBlocked = it.totalTrackersBlocked + trackerInc,
                    totalVideoAdsBlocked = it.totalVideoAdsBlocked + videoAdInc,
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
                videoAdsBlocked = current.videoAdsBlocked + videoAdInc,
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

    fun recordNavigationBlockedEvent(
        tabId: String,
        pageUrl: String,
        targetUrl: String,
        reason: BlockReason,
        host: String
    ): PagePrivacyStats {
        val pageHost = try { Uri.parse(pageUrl).host ?: "" } catch (e: Exception) { "" }
        val hasHttps = pageUrl.startsWith("https://", ignoreCase = true)

        val current = pageStatsMap[tabId] ?: PagePrivacyStats(
            pageUrl = pageUrl,
            pageHost = pageHost,
            isShieldActive = !isDomainAllowed(pageHost),
            hasHttps = hasHttps
        )

        val blockedInfo = BlockedRequestInfo(
            url = targetUrl,
            host = host,
            reason = reason
        )

        _cumulativeStats.update {
            it.copy(
                totalRedirectsBlocked = it.totalRedirectsBlocked + 1,
                totalRequestsIntercepted = it.totalRequestsIntercepted + 1
            )
        }

        val updated = current.copy(
            pageUrl = pageUrl,
            pageHost = pageHost,
            totalRequests = current.totalRequests + 1,
            redirectsBlocked = current.redirectsBlocked + 1,
            blockedDomains = (listOf(blockedInfo) + current.blockedDomains).take(50),
            isShieldActive = !isDomainAllowed(pageHost),
            hasHttps = hasHttps
        )

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

    
    fun recordAiAdDetection(tabId: String, pageUrl: String, elementInfo: String): PagePrivacyStats {
        val current = pageStatsMap[tabId] ?: PagePrivacyStats(pageUrl = pageUrl)
        val detections = current.aiAdDetections.toMutableList()
        detections.add(elementInfo)
        
        val updated = current.copy(
            aiAdsBlocked = current.aiAdsBlocked + 1,
            aiAdDetections = detections
        )
        pageStatsMap[tabId] = updated
        
        _cumulativeStats.value = _cumulativeStats.value.copy(
            totalAiAdsBlocked = _cumulativeStats.value.totalAiAdsBlocked + 1
        )
        return updated
    }

    fun getPageStats(tabId: String): PagePrivacyStats {
        return pageStatsMap[tabId] ?: PagePrivacyStats()
    }
}
