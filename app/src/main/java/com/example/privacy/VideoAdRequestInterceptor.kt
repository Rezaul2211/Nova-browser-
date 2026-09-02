package com.example.privacy

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Intercepts video advertising requests before they reach the player,
 * serving neutralized protocol-compliant responses (empty VAST, empty VMAP, 204 No Content)
 * to ensure web video players transition immediately to the primary video content.
 */
class VideoAdRequestInterceptor(
    val videoAdDetector: VideoAdDetector,
    val vastVmapDetector: VastVmapDetector,
    val manifestInspector: ManifestInspector
) {

    fun shouldIntercept(
        request: WebResourceRequest,
        pageUrl: Uri?,
        videoAdProtectionEnabled: Boolean,
        isDomainAllowed: (String) -> Boolean
    ): InterceptResult {
        if (!videoAdProtectionEnabled) return InterceptResult.PassThrough

        val requestUrl = request.url ?: return InterceptResult.PassThrough
        val host = requestUrl.host?.lowercase() ?: return InterceptResult.PassThrough
        val pageHost = pageUrl?.host?.lowercase() ?: ""

        if (pageHost.isNotBlank() && isDomainAllowed(pageHost)) {
            return InterceptResult.PassThrough
        }

        val eval = videoAdDetector.evaluate(
            requestUrl = requestUrl,
            pageUrl = pageUrl,
            headers = request.requestHeaders
        )

        if (!eval.isVideoAd) {
            return InterceptResult.PassThrough
        }

        // 1. VAST / VMAP Protocol requests -> Return empty XML to immediately skip ad break
        if (eval.isVastVmap) {
            val urlStr = requestUrl.toString().lowercase()
            val response = if (urlStr.contains("vmap")) {
                vastVmapDetector.createEmptyVmapResponse()
            } else if (urlStr.contains(".json") || request.requestHeaders?.get("Accept")?.contains("json") == true) {
                vastVmapDetector.createEmptyJsonAdResponse()
            } else {
                vastVmapDetector.createEmptyVastResponse()
            }
            return InterceptResult.Blocked(response, eval.reason ?: BlockReason.VIDEO_AD, eval.classification)
        }

        // 2. Tracking Beacons & Telemetry -> Return 204 No Content
        if (eval.isTrackingBeacon || requestUrl.path?.contains("gen_204") == true) {
            val emptyStream = ByteArrayInputStream(ByteArray(0))
            val response = WebResourceResponse("text/plain", "UTF-8", emptyStream).apply {
                setStatusCodeAndReasonPhrase(204, "No Content")
                setResponseHeaders(
                    mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Cache-Control" to "no-cache, no-store, must-revalidate"
                    )
                )
            }
            return InterceptResult.Blocked(response, eval.reason ?: BlockReason.VIDEO_AD, eval.classification)
        }

        // 3. Platform video ad endpoints (YouTube / Vimeo / Dailymotion ad break JSON)
        if (requestUrl.path?.contains("ad_break") == true || requestUrl.path?.contains("get_midroll_info") == true) {
            val response = vastVmapDetector.createEmptyJsonAdResponse()
            return InterceptResult.Blocked(response, eval.reason ?: BlockReason.VIDEO_AD, eval.classification)
        }

        // 4. Video ad stream / media segment / ad loader script -> Return empty response to cancel load
        val response = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))).apply {
            setResponseHeaders(mapOf("Access-Control-Allow-Origin" to "*"))
        }
        return InterceptResult.Blocked(response, eval.reason ?: BlockReason.VIDEO_AD, eval.classification)
    }

    sealed class InterceptResult {
        object PassThrough : InterceptResult()
        data class Blocked(
            val response: WebResourceResponse,
            val reason: BlockReason,
            val classification: MediaClassification
        ) : InterceptResult()
    }
}
