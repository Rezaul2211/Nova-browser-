package com.example.privacy

import android.net.Uri

sealed interface NavigationDecision {
    data class Allow(val reason: String = "Legitimate Navigation") : NavigationDecision
    data class Block(
        val reason: BlockReason,
        val targetHost: String,
        val explanation: String
    ) : NavigationDecision
    data class ExternalIntent(val uri: Uri) : NavigationDecision
}

class NavigationProtectionEngine(
    private val isDomainAllowedProvider: (String?) -> Boolean
) {
    constructor(filterEngine: FilterEngine) : this({ filterEngine.isDomainAllowed(it) })
    constructor(adBlockEngine: AdBlockEngine) : this({ adBlockEngine.isDomainAllowed(it) })

    private val ruleManager = FilterRuleManager()

    /**
     * Evaluates whether a proposed navigation request should be allowed or blocked.
     * Core Rule: Never navigate to an unrelated third-party website without an explicit user action.
     *
     * @param targetUri The destination URI being requested.
     * @param currentUri The current page URI hosting the session.
     * @param hasUserGesture Whether the navigation was accompanied by an explicit user gesture (e.g. touch/click).
     * @param isRedirect Whether the request is a redirect (server 3xx or client-side meta/JS redirect).
     * @param isUserDirectAction Whether the action was initiated directly from the browser UI (e.g. address bar, bookmark).
     */
    fun evaluateNavigation(
        targetUri: Uri,
        currentUri: Uri?,
        hasUserGesture: Boolean,
        isRedirect: Boolean,
        isUserDirectAction: Boolean = false
    ): NavigationDecision {
        val scheme = targetUri.scheme?.lowercase() ?: ""

        // 1. Direct browser UI navigation (address bar, search query, bookmarks, history)
        if (isUserDirectAction) {
            val host = targetUri.host?.lowercase() ?: ""
            if (isKnownMaliciousOrGambling(host)) {
                return NavigationDecision.Block(
                    reason = BlockReason.GAMBLING_SPAM,
                    targetHost = host,
                    explanation = "Blocked known harmful or gambling destination: $host"
                )
            }
            return NavigationDecision.Allow("User direct address bar navigation")
        }

        // 2. Safe internal / protocol schemes
        if (scheme == "about" || scheme == "javascript" || scheme == "data" || scheme == "blob") {
            return NavigationDecision.Allow("Internal browser scheme")
        }

        // 3. External application schemes (e.g. mailto, tel, sms, geo, intent)
        if (scheme == "mailto" || scheme == "tel" || scheme == "sms" || scheme == "geo") {
            return if (hasUserGesture) {
                NavigationDecision.ExternalIntent(targetUri)
            } else {
                NavigationDecision.Block(
                    reason = BlockReason.UNSOLICITED_REDIRECT,
                    targetHost = scheme,
                    explanation = "Blocked unsolicited external protocol redirect ($scheme)"
                )
            }
        }

        if (scheme == "intent" || scheme == "market") {
            return if (hasUserGesture && !isHighRiskQuery(targetUri.toString())) {
                NavigationDecision.ExternalIntent(targetUri)
            } else {
                NavigationDecision.Block(
                    reason = BlockReason.UNSOLICITED_REDIRECT,
                    targetHost = targetUri.host ?: scheme,
                    explanation = "Blocked unsolicited app launch intent without user consent"
                )
            }
        }

        // Fallback check for standard HTTP/HTTPS schemes
        if (scheme != "http" && scheme != "https") {
            return NavigationDecision.Block(
                reason = BlockReason.UNSOLICITED_REDIRECT,
                targetHost = scheme,
                explanation = "Blocked unsupported or unsafe protocol: $scheme"
            )
        }

        val targetHost = targetUri.host?.lowercase() ?: ""
        val currentHost = currentUri?.host?.lowercase() ?: ""

        // If target host is empty or null, allow relative navigation
        if (targetHost.isBlank()) {
            return NavigationDecision.Allow("Relative path navigation")
        }

        // Site is explicitly whitelisted by the user
        if (isDomainAllowedProvider(targetHost) || isDomainAllowedProvider(currentHost)) {
            return NavigationDecision.Allow("Domain is user-allowlisted")
        }

        val hostClass = ruleManager.classifyHost(targetHost)

        // 4. BLOCK: Known Malicious, Gambling, Betting, Casino, Ad-Redirect Networks
        if (hostClass == FilterRuleManager.HostClassification.MALICIOUS || isKnownMaliciousRedirect(targetHost)) {
            return NavigationDecision.Block(
                reason = BlockReason.MALICIOUS_REDIRECT,
                targetHost = targetHost,
                explanation = "Blocked malicious ad redirect network: $targetHost"
            )
        }

        if (hostClass == FilterRuleManager.HostClassification.GAMBLING || isKnownGamblingOrScam(targetHost) || isHighRiskQuery(targetUri.toString())) {
            return NavigationDecision.Block(
                reason = BlockReason.GAMBLING_SPAM,
                targetHost = targetHost,
                explanation = "Blocked gambling, betting, or scam website: $targetHost"
            )
        }

        // 5. ALLOW: First-Party / Same-Origin / Subdomain Navigation
        if (currentHost.isBlank() || isSameRegisteredDomain(currentHost, targetHost)) {
            return NavigationDecision.Allow("First-party same-origin navigation")
        }

        // 6. ALLOW: Legitimate Cross-Origin Authentication & Identity Providers (OAuth / SSO)
        if (hostClass == FilterRuleManager.HostClassification.AUTH_WHITELIST || isWhitelistedAuthProvider(targetHost)) {
            return NavigationDecision.Allow("Whitelisted OAuth/SSO Authentication provider ($targetHost)")
        }

        // 7. ALLOW: Legitimate Cross-Origin Payment Gateways & Checkout Flows
        if (hostClass == FilterRuleManager.HostClassification.PAYMENT_WHITELIST || isWhitelistedPaymentGateway(targetHost)) {
            return NavigationDecision.Allow("Whitelisted Payment/Checkout gateway ($targetHost)")
        }

        // 8. Third-Party External Navigation Decision:
        // Rule: If the user explicitly clicked/tapped a link (hasUserGesture == true) and not a redirect trap
        if (hasUserGesture && !isRedirect) {
            return NavigationDecision.Allow("User-initiated external link click to $targetHost")
        }

        // 9. BLOCK: Unsolicited Cross-Origin Redirects, Pop-unders, & Script Navigation Traps
        return NavigationDecision.Block(
            reason = BlockReason.UNSOLICITED_REDIRECT,
            targetHost = targetHost,
            explanation = "Blocked unsolicited background redirect from $currentHost to $targetHost"
        )
    }

    private fun isKnownMaliciousRedirect(host: String): Boolean {
        if (FilterRules.MALICIOUS_REDIRECT_HOSTS.contains(host)) return true
        if (FilterRules.MALICIOUS_REDIRECT_HOSTS.any { host.endsWith(".$it") }) return true
        if (FilterRules.AD_HOSTS.contains(host)) return true
        return FilterRules.AD_HOSTS.any { host.endsWith(".$it") }
    }

    private fun isKnownGamblingOrScam(host: String): Boolean {
        if (FilterRules.GAMBLING_BETTING_HOSTS.contains(host)) return true
        if (FilterRules.GAMBLING_BETTING_HOSTS.any { host.endsWith(".$it") }) return true
        return FilterRules.HIGH_RISK_KEYWORDS.any { keyword -> host.contains(keyword) }
    }

    private fun isKnownMaliciousOrGambling(host: String): Boolean {
        return isKnownMaliciousRedirect(host) || isKnownGamblingOrScam(host)
    }

    private fun isHighRiskQuery(url: String): Boolean {
        val lower = url.lowercase()
        return FilterRules.HIGH_RISK_KEYWORDS.any { kw ->
            lower.contains("=$kw") || lower.contains("/$kw") || lower.contains("&$kw")
        }
    }

    private fun isWhitelistedAuthProvider(host: String): Boolean {
        if (FilterRules.WHITELISTED_AUTH_HOSTS.contains(host)) return true
        return FilterRules.WHITELISTED_AUTH_HOSTS.any { host.endsWith(".$it") }
    }

    private fun isWhitelistedPaymentGateway(host: String): Boolean {
        if (FilterRules.WHITELISTED_PAYMENT_HOSTS.contains(host)) return true
        return FilterRules.WHITELISTED_PAYMENT_HOSTS.any { host.endsWith(".$it") }
    }

    private fun isSameRegisteredDomain(hostA: String, hostB: String): Boolean {
        if (hostA == hostB) return true
        if (hostA.endsWith(".$hostB") || hostB.endsWith(".$hostA")) return true
        val domainA = getRegisteredDomain(hostA)
        val domainB = getRegisteredDomain(hostB)
        return domainA.isNotBlank() && domainA == domainB
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
