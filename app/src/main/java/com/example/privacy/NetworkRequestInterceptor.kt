package com.example.privacy

import android.net.Uri

enum class ResourceType {
    DOCUMENT,
    SCRIPT,
    IMAGE,
    STYLESHEET,
    IFRAME,
    XHR_FETCH,
    MEDIA,
    WEBSOCKET,
    FONT,
    OTHER
}

data class RequestEvaluation(
    val shouldBlock: Boolean,
    val reason: BlockReason? = null,
    val isThirdParty: Boolean = false,
    val host: String = "",
    val resourceType: ResourceType = ResourceType.OTHER
)

/**
 * Intercepts network/resource requests before loading, classifies request type,
 * and performs precision filtering against ads, trackers, and video ad streams.
 */
class NetworkRequestInterceptor(
    private val ruleManager: FilterRuleManager
) {

    fun classifyResourceType(requestUrl: Uri, headers: Map<String, String>? = null): ResourceType {
        val path = requestUrl.path?.lowercase() ?: ""
        val accept = headers?.get("Accept")?.lowercase() ?: headers?.get("accept")?.lowercase() ?: ""
        val dest = headers?.get("Sec-Fetch-Dest")?.lowercase() ?: headers?.get("sec-fetch-dest")?.lowercase() ?: ""

        if (dest.isNotEmpty()) {
            return when (dest) {
                "document" -> ResourceType.DOCUMENT
                "script" -> ResourceType.SCRIPT
                "image" -> ResourceType.IMAGE
                "style" -> ResourceType.STYLESHEET
                "iframe" -> ResourceType.IFRAME
                "empty" -> ResourceType.XHR_FETCH
                "video", "audio" -> ResourceType.MEDIA
                "font" -> ResourceType.FONT
                "websocket" -> ResourceType.WEBSOCKET
                else -> ResourceType.OTHER
            }
        }

        if (path.endsWith(".js") || accept.contains("javascript")) return ResourceType.SCRIPT
        if (path.endsWith(".css") || accept.contains("text/css")) return ResourceType.STYLESHEET
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".webp") || path.endsWith(".gif") || path.endsWith(".svg") ||
            path.endsWith(".ico") || accept.contains("image/")
        ) return ResourceType.IMAGE
        if (path.endsWith(".mp4") || path.endsWith(".m3u8") || path.endsWith(".mpd") ||
            path.endsWith(".ts") || path.endsWith(".webm") || path.endsWith(".m4s") ||
            accept.contains("video/") || accept.contains("audio/")
        ) return ResourceType.MEDIA
        if (path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".otf")) return ResourceType.FONT
        if (accept.contains("text/html") || accept.contains("application/xhtml+xml")) return ResourceType.DOCUMENT
        if (accept.contains("application/json") || accept.contains("application/xml") || accept.contains("text/xml")) return ResourceType.XHR_FETCH

        return ResourceType.OTHER
    }

    /**
     * Inspects an outgoing network request and decides whether to block it.
     */
    fun evaluateRequest(
        requestUrl: Uri,
        pageUrl: Uri?,
        adBlockingEnabled: Boolean,
        trackerBlockingEnabled: Boolean,
        videoAdProtectionEnabled: Boolean,
        headers: Map<String, String>? = null,
        isDomainAllowed: (String) -> Boolean
    ): RequestEvaluation {
        val requestHost = requestUrl.host?.lowercase() ?: return RequestEvaluation(false)
        val pageHost = pageUrl?.host?.lowercase() ?: ""
        val resourceType = classifyResourceType(requestUrl, headers)
        val urlString = requestUrl.toString()

        // If the top-level site is explicitly allowed/whitelisted by the user, pass through
        if (pageHost.isNotBlank() && isDomainAllowed(pageHost)) {
            return RequestEvaluation(
                shouldBlock = false,
                isThirdParty = false,
                host = requestHost,
                resourceType = resourceType
            )
        }

        val isThirdParty = pageHost.isNotBlank() && !isSameRegisteredDomain(pageHost, requestHost)

        // 1. Guard against breaking legitimate media playback
        if (ruleManager.isLegitimateVideoPlaybackStream(urlString)) {
            return RequestEvaluation(
                shouldBlock = false,
                isThirdParty = isThirdParty,
                host = requestHost,
                resourceType = ResourceType.MEDIA
            )
        }

        val hostClass = ruleManager.classifyHost(requestHost)

        // 2. Pass legitimate Auth & Payment gateways
        if (hostClass == FilterRuleManager.HostClassification.AUTH_WHITELIST ||
            hostClass == FilterRuleManager.HostClassification.PAYMENT_WHITELIST
        ) {
            return RequestEvaluation(
                shouldBlock = false,
                isThirdParty = isThirdParty,
                host = requestHost,
                resourceType = resourceType
            )
        }

        // 3. Video Ad & Stream Ad Filtering
        if (videoAdProtectionEnabled) {
            if (hostClass == FilterRuleManager.HostClassification.VIDEO_AD) {
                return RequestEvaluation(
                    shouldBlock = true,
                    reason = BlockReason.VIDEO_AD,
                    isThirdParty = isThirdParty,
                    host = requestHost,
                    resourceType = resourceType
                )
            }

            for (pattern in ruleManager.videoAdPathPatterns) {
                if (pattern.matches(urlString)) {
                    return RequestEvaluation(
                        shouldBlock = true,
                        reason = BlockReason.VIDEO_AD,
                        isThirdParty = isThirdParty,
                        host = requestHost,
                        resourceType = resourceType
                    )
                }
            }
        }

        // 4. General Ad Network Filtering
        if (adBlockingEnabled) {
            if (hostClass == FilterRuleManager.HostClassification.AD ||
                hostClass == FilterRuleManager.HostClassification.MALICIOUS ||
                hostClass == FilterRuleManager.HostClassification.GAMBLING
            ) {
                val reason = when (hostClass) {
                    FilterRuleManager.HostClassification.GAMBLING -> BlockReason.GAMBLING_SPAM
                    FilterRuleManager.HostClassification.MALICIOUS -> BlockReason.MALICIOUS_REDIRECT
                    else -> BlockReason.AD_NETWORK
                }
                return RequestEvaluation(
                    shouldBlock = true,
                    reason = reason,
                    isThirdParty = isThirdParty,
                    host = requestHost,
                    resourceType = resourceType
                )
            }
        }

        // 5. Tracker & Analytics Filtering
        if (trackerBlockingEnabled) {
            if (hostClass == FilterRuleManager.HostClassification.TRACKER) {
                return RequestEvaluation(
                    shouldBlock = true,
                    reason = BlockReason.TRACKER,
                    isThirdParty = isThirdParty,
                    host = requestHost,
                    resourceType = resourceType
                )
            }
        }

        // 6. Path and Query Pattern Filtering
        if (adBlockingEnabled || trackerBlockingEnabled) {
            for (pattern in ruleManager.adPathPatterns) {
                if (pattern.matches(urlString)) {
                    val reason = if (isThirdParty) BlockReason.THIRD_PARTY_RESTRICTION else BlockReason.AD_NETWORK
                    return RequestEvaluation(
                        shouldBlock = true,
                        reason = reason,
                        isThirdParty = isThirdParty,
                        host = requestHost,
                        resourceType = resourceType
                    )
                }
            }
        }

        return RequestEvaluation(
            shouldBlock = false,
            isThirdParty = isThirdParty,
            host = requestHost,
            resourceType = resourceType
        )
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
