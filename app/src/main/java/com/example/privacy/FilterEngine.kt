package com.example.privacy

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

class FilterEngine {

    val adBlockEngine = AdBlockEngine()

    val cumulativeStats: StateFlow<CumulativePrivacyStats> = adBlockEngine.cumulativeStats
    val navigationProtectionEngine: NavigationProtectionEngine = adBlockEngine.navigationProtectionEngine
    val videoAdRequestInterceptor: VideoAdRequestInterceptor = adBlockEngine.videoAdRequestInterceptor
    val adNavigationGuard: AdNavigationGuard = adBlockEngine.adNavigationGuard

    fun setAllowedDomains(domains: Set<String>) {
        adBlockEngine.setAllowedDomains(domains)
    }

    fun isDomainAllowed(host: String?): Boolean {
        return adBlockEngine.isDomainAllowed(host)
    }

    data class FilterResult(
        val shouldBlock: Boolean,
        val reason: BlockReason? = null,
        val isThirdParty: Boolean = false,
        val host: String = "",
        val resourceType: ResourceType = ResourceType.OTHER
    )

    /**
     * Inspects an outgoing web resource request and determines if it should be blocked.
     */
    fun shouldBlockRequest(
        requestUrl: Uri,
        pageUrl: Uri?,
        adBlockingEnabled: Boolean,
        trackerBlockingEnabled: Boolean,
        videoAdProtectionEnabled: Boolean = true,
        headers: Map<String, String>? = null
    ): FilterResult {
        val eval = adBlockEngine.evaluateNetworkRequest(
            requestUrl = requestUrl,
            pageUrl = pageUrl,
            adBlockingEnabled = adBlockingEnabled,
            trackerBlockingEnabled = trackerBlockingEnabled,
            videoAdProtectionEnabled = videoAdProtectionEnabled,
            headers = headers
        )
        return FilterResult(
            shouldBlock = eval.shouldBlock,
            reason = eval.reason,
            isThirdParty = eval.isThirdParty,
            host = eval.host,
            resourceType = eval.resourceType
        )
    }

    fun recordRequestEvent(
        tabId: String,
        pageUrl: String,
        requestUrl: String,
        result: FilterResult
    ): PagePrivacyStats {
        val eval = RequestEvaluation(
            shouldBlock = result.shouldBlock,
            reason = result.reason,
            isThirdParty = result.isThirdParty,
            host = result.host,
            resourceType = result.resourceType
        )
        return adBlockEngine.recordRequestEvent(tabId, pageUrl, requestUrl, eval)
    }

    fun recordNavigationBlockedEvent(
        tabId: String,
        pageUrl: String,
        targetUrl: String,
        reason: BlockReason,
        host: String
    ): PagePrivacyStats {
        return adBlockEngine.recordNavigationBlockedEvent(tabId, pageUrl, targetUrl, reason, host)
    }

    fun resetTabStats(tabId: String, newUrl: String) {
        adBlockEngine.resetTabStats(tabId, newUrl)
    }

    fun getPageStats(tabId: String): PagePrivacyStats {
        return adBlockEngine.getPageStats(tabId)
    }
}
