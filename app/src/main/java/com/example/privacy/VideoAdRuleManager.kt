package com.example.privacy

import android.net.Uri

/**
 * Modular and updateable rule management for video advertising,
 * VAST/VMAP protocols, streaming ad servers, and video player ad loaders.
 */
class VideoAdRuleManager {

    // 1. Dedicated Video Ad Serving & Header Bidding Hosts
    val videoAdHosts: Set<String> = setOf(
        "imasdk.googleapis.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "static.doubleclick.net",
        "ad.doubleclick.net",
        "video-stats.l.doubleclick.net",
        "spotxchange.com",
        "spotx.tv",
        "search.spotxchange.com",
        "springserve.com",
        "video.springserve.com",
        "freewheel.tv",
        "fwmrm.net",
        "innovid.com",
        "s.innovid.com",
        "teads.tv",
        "sync.teads.tv",
        "tremorhub.com",
        "connatix.com",
        "cdns.connatix.com",
        "primis.tech",
        "live.primis.tech",
        "vidoomy.com",
        "vidazoo.com",
        "aniview.com",
        "gov.aniview.com",
        "streamrail.com",
        "smartclip.net",
        "smartclip.tv",
        "targetspot.com",
        "adsupply.com",
        "playwire.com",
        "brid.tv",
        "vdo.ai",
        "mediavine.com",
        "anyclip.com",
        "unruly.co",
        "undertone.com",
        "streamamp.com",
        "ex.streamamp.com",
        "monetize.outbrain.com",
        "trc.taboola.com"
    )

    // 2. YouTube & Major Video Platform Ad Endpoints
    val platformVideoAdPatterns: List<Regex> = listOf(
        Regex(".*/api/stats/ads.*", RegexOption.IGNORE_CASE),
        Regex(".*/api/stats/atr.*", RegexOption.IGNORE_CASE),
        Regex(".*/api/stats/qoe\\?.*adformat.*", RegexOption.IGNORE_CASE),
        Regex(".*/ptracking.*", RegexOption.IGNORE_CASE),
        Regex(".*/get_midroll_info.*", RegexOption.IGNORE_CASE),
        Regex(".*/pagead/.*", RegexOption.IGNORE_CASE),
        Regex(".*/pagead/lvz.*", RegexOption.IGNORE_CASE),
        Regex(".*/pagead/gen_204.*", RegexOption.IGNORE_CASE),
        Regex(".*/youtubei/v1/player/ad_break.*", RegexOption.IGNORE_CASE),
        Regex(".*/youtubei/v1/att/get.*", RegexOption.IGNORE_CASE),
        Regex(".*googlevideo\\.com/videoplayback.*[&?]adformat=.*", RegexOption.IGNORE_CASE),
        Regex(".*googlevideo\\.com/videoplayback.*[&?]ctier=.*", RegexOption.IGNORE_CASE),
        Regex(".*googlevideo\\.com/videoplayback.*[&?]ad_type=.*", RegexOption.IGNORE_CASE)
    )

    // 3. VAST / VMAP & Video Ad Tag URL Signatures
    val vastVmapPatterns: List<Regex> = listOf(
        Regex(".*/vast(\\.xml|/|\\?|\\.php).*", RegexOption.IGNORE_CASE),
        Regex(".*/vmap(\\.xml|/|\\?|\\.php).*", RegexOption.IGNORE_CASE),
        Regex(".*/vpaid(\\.js|/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/gampad/ads.*", RegexOption.IGNORE_CASE),
        Regex(".*/gampad/live/ads.*", RegexOption.IGNORE_CASE),
        Regex(".*/video/ads?(/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_tag=.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]ad_tag_url=.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]sz=640x480.*&impl=s.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]env=vp.*&gdfp_req=1.*", RegexOption.IGNORE_CASE),
        Regex(".*/preroll(\\.xml|/|\\?|\\.php).*", RegexOption.IGNORE_CASE),
        Regex(".*/midroll(\\.xml|/|\\?|\\.php).*", RegexOption.IGNORE_CASE),
        Regex(".*/postroll(\\.xml|/|\\?|\\.php).*", RegexOption.IGNORE_CASE)
    )

    // 4. Video Ad Tracking & Beacon URL Signatures
    val adTrackingPatterns: List<Regex> = listOf(
        Regex(".*[?&]correlator=\\d+.*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_tracker.*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_impression.*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_event.*", RegexOption.IGNORE_CASE),
        Regex(".*/video_ad_click.*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_quartile.*", RegexOption.IGNORE_CASE),
        Regex(".*/ad_completed.*", RegexOption.IGNORE_CASE)
    )

    fun matchesVideoAdHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val clean = host.lowercase()
        if (videoAdHosts.contains(clean)) return true
        return videoAdHosts.any { clean.endsWith(".$it") }
    }

    fun matchesPlatformVideoAd(url: String): Boolean {
        return platformVideoAdPatterns.any { it.matches(url) }
    }

    fun matchesVastVmap(url: String): Boolean {
        return vastVmapPatterns.any { it.matches(url) }
    }

    fun matchesAdTracking(url: String): Boolean {
        return adTrackingPatterns.any { it.matches(url) }
    }
}
