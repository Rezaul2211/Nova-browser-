package com.example.privacy

import android.net.Uri

enum class MediaClassification {
    LEGITIMATE_CONTENT_STREAM,
    LEGITIMATE_SUBTITLES,
    LEGITIMATE_THUMBNAIL,
    LEGITIMATE_PLAYER_SCRIPT,
    VAST_VMAP_AD_TAG,
    VIDEO_AD_NETWORK_REQUEST,
    VIDEO_AD_MEDIA_STREAM,
    VIDEO_AD_TRACKING_BEACON,
    VIDEO_AD_MANIFEST_INJECTION,
    UNKNOWN
}

/**
 * Classifies all media-related network requests to prevent blocking
 * legitimate video/audio while precisely isolating video ad streams.
 */
class MediaRequestClassifier(
    private val ruleManager: VideoAdRuleManager
) {

    fun isLegitimateContent(url: String, host: String): Boolean {
        val lowerUrl = url.lowercase()
        val lowerHost = host.lowercase()

        // 1. YouTube / Google Video content streams (ensure no ad tags attached)
        if (lowerHost.endsWith("googlevideo.com") && lowerUrl.contains("/videoplayback")) {
            if (!lowerUrl.contains("adformat=") && !lowerUrl.contains("ctier=") && !lowerUrl.contains("ad_type=")) {
                return true
            }
        }

        // 2. Standard video subtitle formats
        if (lowerUrl.endsWith(".vtt") || lowerUrl.endsWith(".srt") || lowerUrl.contains(".vtt?") || lowerUrl.contains(".srt?")) {
            return true
        }

        // 3. Known video player open-source libraries / CDNs
        if (lowerUrl.contains("vjs.zencdn.net") || lowerUrl.contains("cdn.jsdelivr.net/npm/hls.js") ||
            lowerUrl.contains("cdn.jsdelivr.net/npm/shaka-player") || lowerUrl.contains("cdnjs.cloudflare.com/ajax/libs/video.js") ||
            lowerUrl.contains("cdn.jwplayer.com/libraries") || lowerUrl.contains("plyr.io")
        ) {
            return true
        }

        return false
    }

    fun classify(url: String, host: String, headers: Map<String, String>? = null): MediaClassification {
        val lowerUrl = url.lowercase()
        val lowerHost = host.lowercase()

        // Check legitimate exemptions first
        if (isLegitimateContent(url, host)) {
            return if (lowerUrl.endsWith(".vtt") || lowerUrl.endsWith(".srt")) {
                MediaClassification.LEGITIMATE_SUBTITLES
            } else {
                MediaClassification.LEGITIMATE_CONTENT_STREAM
            }
        }

        // Check VAST/VMAP protocols
        if (ruleManager.matchesVastVmap(url)) {
            return MediaClassification.VAST_VMAP_AD_TAG
        }

        // Check platform specific video ads (YouTube ad breaks, tracking)
        if (ruleManager.matchesPlatformVideoAd(url)) {
            return MediaClassification.VIDEO_AD_NETWORK_REQUEST
        }

        // Check video ad tracking beacons
        if (ruleManager.matchesAdTracking(url)) {
            return MediaClassification.VIDEO_AD_TRACKING_BEACON
        }

        // Check video ad network hosts
        if (ruleManager.matchesVideoAdHost(lowerHost)) {
            return MediaClassification.VIDEO_AD_NETWORK_REQUEST
        }

        // Check if HLS manifest with ad signatures
        if (lowerUrl.endsWith(".m3u8") || lowerUrl.contains(".m3u8?")) {
            if (lowerUrl.contains("ad_") || lowerUrl.contains("preroll") || lowerUrl.contains("midroll") || lowerUrl.contains("sponsor")) {
                return MediaClassification.VIDEO_AD_MANIFEST_INJECTION
            }
            return MediaClassification.LEGITIMATE_CONTENT_STREAM
        }

        // Check if standard video chunk/stream
        if (lowerUrl.endsWith(".ts") || lowerUrl.endsWith(".m4s") || lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".webm")) {
            // Check if segment is explicitly from an ad host or ad path
            if (ruleManager.matchesVideoAdHost(lowerHost) || lowerUrl.contains("/ads/") || lowerUrl.contains("/ad_segment/")) {
                return MediaClassification.VIDEO_AD_MEDIA_STREAM
            }
            return MediaClassification.LEGITIMATE_CONTENT_STREAM
        }

        return MediaClassification.UNKNOWN
    }
}
