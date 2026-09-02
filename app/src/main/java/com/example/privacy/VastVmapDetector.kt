package com.example.privacy

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Detects and neutralizes VAST (Video Ad Serving Template) and
 * VMAP (Video Multiple Ad Playlist) ad requests by generating standard-compliant
 * empty ad responses that trigger immediate player progression to content.
 */
class VastVmapDetector(
    private val ruleManager: VideoAdRuleManager
) {

    companion object {
        const val EMPTY_VAST_XML = """<?xml version="1.0" encoding="UTF-8"?>
<VAST version="4.2">
</VAST>"""

        const val EMPTY_VMAP_XML = """<?xml version="1.0" encoding="UTF-8"?>
<vmap:VMAP xmlns:vmap="http://www.iab.net/videosuite/vmap" version="1.0">
</vmap:VMAP>"""

        const val EMPTY_JSON_AD_RESPONSE = """{"ads":[],"adPlacements":[],"status":"ok"}"""
    }

    fun isVastOrVmapRequest(url: String): Boolean {
        return ruleManager.matchesVastVmap(url)
    }

    fun createEmptyVastResponse(): WebResourceResponse {
        val stream = ByteArrayInputStream(EMPTY_VAST_XML.toByteArray(Charsets.UTF_8))
        val response = WebResourceResponse("application/xml", "UTF-8", stream)
        response.setResponseHeaders(
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                "Access-Control-Allow-Headers" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        )
        return response
    }

    fun createEmptyVmapResponse(): WebResourceResponse {
        val stream = ByteArrayInputStream(EMPTY_VMAP_XML.toByteArray(Charsets.UTF_8))
        val response = WebResourceResponse("application/xml", "UTF-8", stream)
        response.setResponseHeaders(
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                "Access-Control-Allow-Headers" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        )
        return response
    }

    fun createEmptyJsonAdResponse(): WebResourceResponse {
        val stream = ByteArrayInputStream(EMPTY_JSON_AD_RESPONSE.toByteArray(Charsets.UTF_8))
        val response = WebResourceResponse("application/json", "UTF-8", stream)
        response.setResponseHeaders(
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            )
        )
        return response
    }
}
