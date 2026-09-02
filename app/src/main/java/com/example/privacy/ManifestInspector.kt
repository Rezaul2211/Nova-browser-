package com.example.privacy

import java.io.BufferedReader
import java.io.StringReader

/**
 * Inspects HLS (.m3u8) playlists to detect and remove server-side or
 * client-spliced pre-roll and mid-roll ad segments while preserving content.
 */
class ManifestInspector(
    private val ruleManager: VideoAdRuleManager
) {

    /**
     * Cleanses an HLS M3U8 manifest content string by removing ad blocks.
     */
    fun filterHlsManifest(rawManifest: String): String {
        if (!rawManifest.contains("#EXTM3U")) return rawManifest

        val reader = BufferedReader(StringReader(rawManifest))
        val output = StringBuilder()
        var line: String?
        var inAdCueBlock = false

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line!!.trim()

            // Detect SCTE-35 / CUE Ad Out start
            if (currentLine.startsWith("#EXT-X-CUE-OUT") ||
                currentLine.startsWith("#EXT-OATCLS-SCTE35") ||
                currentLine.startsWith("#EXT-X-DATERANGE:ID=\"ad-") ||
                currentLine.startsWith("#EXT-X-DATERANGE:CLASS=\"ad-")
            ) {
                inAdCueBlock = true
                continue
            }

            // Detect CUE Ad In (End of ad block)
            if (currentLine.startsWith("#EXT-X-CUE-IN")) {
                inAdCueBlock = false
                continue
            }

            // If inside ad cue block, skip ad segments and tags
            if (inAdCueBlock) {
                // If we reach a discontinuity or end of ad markers, check if content resumes
                if (currentLine.startsWith("#EXTINF") || (!currentLine.startsWith("#") && currentLine.isNotEmpty())) {
                    continue
                }
            }

            // Check if individual segment URL belongs to a known video ad network
            if (!currentLine.startsWith("#") && currentLine.isNotEmpty()) {
                if (ruleManager.matchesVideoAdHost(currentLine) ||
                    currentLine.contains("/ad_segment/") ||
                    currentLine.contains("/preroll_segment/")
                ) {
                    continue
                }
            }

            output.append(currentLine).append("\n")
        }

        return output.toString()
    }
}
