package com.example.privacy

import android.net.Uri

data class VideoAdEvaluation(
    val isVideoAd: Boolean,
    val classification: MediaClassification,
    val reason: BlockReason? = null,
    val isVastVmap: Boolean = false,
    val isTrackingBeacon: Boolean = false,
    val isManifestAd: Boolean = false
)

/**
 * Dedicated Video Ad Detector analyzing media URLs, query parameters,
 * headers, VAST/VMAP protocols, and platform-specific signatures.
 */
class VideoAdDetector(
    val ruleManager: VideoAdRuleManager,
    val classifier: MediaRequestClassifier,
    val vastVmapDetector: VastVmapDetector
) {

    fun evaluate(
        requestUrl: Uri,
        pageUrl: Uri?,
        headers: Map<String, String>? = null
    ): VideoAdEvaluation {
        val urlString = requestUrl.toString()
        val host = requestUrl.host?.lowercase() ?: ""

        // 1. Check if definitely legitimate media (e.g. googlevideo content without ad markers)
        if (classifier.isLegitimateContent(urlString, host)) {
            return VideoAdEvaluation(
                isVideoAd = false,
                classification = MediaClassification.LEGITIMATE_CONTENT_STREAM
            )
        }

        // 2. Check for VAST / VMAP ad tag protocols
        if (vastVmapDetector.isVastOrVmapRequest(urlString)) {
            return VideoAdEvaluation(
                isVideoAd = true,
                classification = MediaClassification.VAST_VMAP_AD_TAG,
                reason = BlockReason.VIDEO_AD,
                isVastVmap = true
            )
        }

        // 3. Classify through media classifier
        val classification = classifier.classify(urlString, host, headers)

        return when (classification) {
            MediaClassification.VAST_VMAP_AD_TAG -> VideoAdEvaluation(
                isVideoAd = true,
                classification = classification,
                reason = BlockReason.VIDEO_AD,
                isVastVmap = true
            )
            MediaClassification.VIDEO_AD_NETWORK_REQUEST -> VideoAdEvaluation(
                isVideoAd = true,
                classification = classification,
                reason = BlockReason.VIDEO_AD
            )
            MediaClassification.VIDEO_AD_MEDIA_STREAM -> VideoAdEvaluation(
                isVideoAd = true,
                classification = classification,
                reason = BlockReason.VIDEO_AD
            )
            MediaClassification.VIDEO_AD_TRACKING_BEACON -> VideoAdEvaluation(
                isVideoAd = true,
                classification = classification,
                reason = BlockReason.VIDEO_AD,
                isTrackingBeacon = true
            )
            MediaClassification.VIDEO_AD_MANIFEST_INJECTION -> VideoAdEvaluation(
                isVideoAd = true,
                classification = classification,
                reason = BlockReason.VIDEO_AD,
                isManifestAd = true
            )
            MediaClassification.LEGITIMATE_CONTENT_STREAM,
            MediaClassification.LEGITIMATE_SUBTITLES,
            MediaClassification.LEGITIMATE_THUMBNAIL,
            MediaClassification.LEGITIMATE_PLAYER_SCRIPT -> VideoAdEvaluation(
                isVideoAd = false,
                classification = classification
            )
            MediaClassification.UNKNOWN -> VideoAdEvaluation(
                isVideoAd = false,
                classification = MediaClassification.UNKNOWN
            )
        }
    }
}
