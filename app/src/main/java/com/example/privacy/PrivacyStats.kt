package com.example.privacy

data class PagePrivacyStats(
    val pageUrl: String = "",
    val pageHost: String = "",
    val totalRequests: Int = 0,
    val adsBlocked: Int = 0,
    val aiAdsBlocked: Int = 0,
    val aiAdDetections: List<String> = emptyList(),
    val trackersBlocked: Int = 0,
    val videoAdsBlocked: Int = 0,
    val thirdPartyBlocked: Int = 0,
    val redirectsBlocked: Int = 0,
    val blockedDomains: List<BlockedRequestInfo> = emptyList(),
    val isShieldActive: Boolean = true,
    val hasHttps: Boolean = true,
    val cookiesCount: Int = 0
)

data class BlockedRequestInfo(
    val url: String,
    val host: String,
    val reason: BlockReason,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BlockReason(val label: String) {
    AD_NETWORK("Ad Network"),
    AI_DETECTED_AD("AI Detected Ad"),
    TRACKER("Tracker / Analytics"),
    VIDEO_AD("Video Ad / Stream Ad"),
    FINGERPRINTING("Fingerprinting Script"),
    MALICIOUS_REDIRECT("Malicious Ad Redirect"),
    UNSOLICITED_REDIRECT("Unsolicited 3rd-Party Redirect"),
    GAMBLING_SPAM("Gambling / Betting / Spam Site"),
    POPUP_HIJACK("Pop-under / New Window Hijack"),
    ANTI_ADBLOCK_NEUTRALIZED("Anti-Adblock Overlay"),
    THIRD_PARTY_RESTRICTION("Third-party Ad Resource"),
    USER_RULE("Custom Rule")
}

data class CumulativePrivacyStats(
    val totalAdsBlocked: Long = 0,
    val totalAiAdsBlocked: Long = 0,
    val totalTrackersBlocked: Long = 0,
    val totalVideoAdsBlocked: Long = 0,
    val totalThirdPartyBlocked: Long = 0,
    val totalRedirectsBlocked: Long = 0,
    val totalRequestsIntercepted: Long = 0
)
