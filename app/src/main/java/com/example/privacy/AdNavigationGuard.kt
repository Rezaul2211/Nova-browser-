package com.example.privacy

import android.net.Uri

/**
 * Guards against unsolicited redirects, new window clickjacking, and pop-unders
 * triggered by video players or malicious interstitial scripts during media playback.
 */
class AdNavigationGuard(
    private val isDomainAllowedProvider: (String?) -> Boolean
) {

    private val ruleManager = FilterRuleManager()
    private val videoAdRuleManager = VideoAdRuleManager()

    fun evaluateNavigation(
        targetUri: Uri,
        currentUri: Uri?,
        isUserGesture: Boolean,
        hasUserInteractedRecently: Boolean
    ): NavigationDecision {
        val targetHost = targetUri.host?.lowercase() ?: ""
        val currentHost = currentUri?.host?.lowercase() ?: ""
        val scheme = targetUri.scheme?.lowercase() ?: ""

        if (targetHost.isBlank()) {
            return if (scheme == "about" || scheme == "data" || scheme == "blob") {
                NavigationDecision.Allow("Local safe scheme")
            } else {
                NavigationDecision.Block(
                    reason = BlockReason.MALICIOUS_REDIRECT,
                    targetHost = "unknown",
                    explanation = "Blank destination host"
                )
            }
        }

        // Allow user-whitelisted sites
        if (isDomainAllowedProvider(targetHost) || isDomainAllowedProvider(currentHost)) {
            return NavigationDecision.Allow("User allowlisted domain")
        }

        val hostClass = ruleManager.classifyHost(targetHost)

        // Pass Whitelisted OAuth & Payments
        if (hostClass == FilterRuleManager.HostClassification.AUTH_WHITELIST ||
            hostClass == FilterRuleManager.HostClassification.PAYMENT_WHITELIST
        ) {
            return NavigationDecision.Allow("Whitelisted Auth or Payment provider")
        }

        // Block Video Ad networks attempting top-level navigation
        if (videoAdRuleManager.matchesVideoAdHost(targetHost)) {
            return NavigationDecision.Block(
                reason = BlockReason.VIDEO_AD,
                targetHost = targetHost,
                explanation = "Video ad network attempted navigation"
            )
        }

        // Block Malicious, Adware & Gambling redirects
        if (hostClass == FilterRuleManager.HostClassification.MALICIOUS ||
            hostClass == FilterRuleManager.HostClassification.GAMBLING ||
            hostClass == FilterRuleManager.HostClassification.AD
        ) {
            val reason = when (hostClass) {
                FilterRuleManager.HostClassification.GAMBLING -> BlockReason.GAMBLING_SPAM
                FilterRuleManager.HostClassification.AD -> BlockReason.AD_NETWORK
                else -> BlockReason.MALICIOUS_REDIRECT
            }
            return NavigationDecision.Block(
                reason = reason,
                targetHost = targetHost,
                explanation = "Blocked malicious / ad network navigation target"
            )
        }

        // Block non-user-gesture cross-origin redirects
        if (!isUserGesture && !hasUserInteractedRecently && currentHost.isNotBlank() && targetHost != currentHost) {
            return NavigationDecision.Block(
                reason = BlockReason.UNSOLICITED_REDIRECT,
                targetHost = targetHost,
                explanation = "Blocked automatic background cross-origin redirect"
            )
        }

        return NavigationDecision.Allow("Navigation permitted")
    }
}
