package com.example.privacy

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages domain lists, regex rules, video ad signatures, VAST/VMAP patterns,
 * high-risk keywords, and legitimate provider whitelists.
 */
class FilterRuleManager {

    // 1. Known General Advertising Hosts
    val adHosts: Set<String> = hashSetOf(
        "doubleclick.net",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "ads.google.com",
        "googlesyndication.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "advertising.com",
        "adcolony.com",
        "unityads.unity3d.com",
        "vungle.com",
        "inmobi.com",
        "applovin.com",
        "ironsrc.com",
        "chartboost.com",
        "admob.com",
        "adtechus.com",
        "adroll.com",
        "adzerk.net",
        "smartadserver.com",
        "media.net",
        "buysellads.com",
        "revcontent.com",
        "zergnet.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "mgid.com",
        "bidswitch.net",
        "casalemedia.com",
        "adform.net",
        "sovrn.com",
        "sharethrough.com",
        "triplelift.com",
        "yieldmo.com",
        "exponential.com",
        "contextweb.com",
        "chitika.com",
        "adpushup.com",
        "adbutler.com",
        "trafficjunky.com",
        "exoclick.com",
        "juicyads.com",
        "ero-advertising.com",
        "adblade.com",
        "bidvertiser.com",
        "infolinks.com",
        "monetag.com",
        "clickadu.com",
        "richaudience.com",
        "undertone.com",
        "conversantmedia.com",
        "dotomi.com",
        "quantserve.com",
        "yandex.ru/ads",
        "an.yandex.ru"
    )

    // 2. Video Advertising, VAST/VMAP, & Pre-roll/Mid-roll Ad Servers
    val videoAdHosts: Set<String> = hashSetOf(
        "imasdk.googleapis.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "static.doubleclick.net",
        "spotxchange.com",
        "spotx.tv",
        "springserve.com",
        "freewheel.tv",
        "fwmrm.net",
        "innovid.com",
        "teads.tv",
        "tremorhub.com",
        "connatix.com",
        "primis.tech",
        "vidoomy.com",
        "vidazoo.com",
        "aniview.com",
        "streamrail.com",
        "smartclip.net",
        "smartclip.tv",
        "targetspot.com",
        "adsupply.com",
        "playwire.com",
        "brid.tv",
        "vdo.ai"
    )

    // 3. Known Trackers & Fingerprinting Endpoints
    val trackerHosts: Set<String> = hashSetOf(
        "google-analytics.com",
        "analytics.google.com",
        "stats.g.doubleclick.net",
        "googletagmanager.com",
        "googletagservices.com",
        "scorecardresearch.com",
        "hotjar.com",
        "clarity.ms",
        "segment.io",
        "segment.com",
        "mixpanel.com",
        "amplitude.com",
        "heapanalytics.com",
        "fullstory.com",
        "crazyegg.com",
        "mouseflow.com",
        "loggly.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "kochava.com",
        "singular.net",
        "facebook.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "static.ads-twitter.com",
        "analytics.tiktok.com",
        "tr.snapchat.com",
        "sc-static.net",
        "bat.bing.com",
        "claritybt.trafficmanager.net",
        "statcounter.com",
        "histats.com",
        "gemius.pl",
        "chartbeat.com",
        "optimizely.com",
        "visualwebsiteoptimizer.com",
        "vwo.com",
        "newrelic.com",
        "nr-data.net",
        "dynatrace.com",
        "sentry.io/api",
        "fingerprintjs.com",
        "fpjs.sh",
        "alexametrics.com",
        "clicky.com",
        "matomo.cloud",
        "piwik.pro",
        "quantcount.com",
        "parsely.com",
        "luckyorange.com",
        "inspectlet.com",
        "sessioncam.com",
        "wootric.com",
        "intercom.io",
        "driftt.com",
        "livechatinc.com"
    )

    // 4. Malicious Redirect, Clickjacking & Scam Networks
    val maliciousRedirectHosts: Set<String> = hashSetOf(
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "onclickads.net",
        "adsterra.com",
        "hilltopads.net",
        "exoclick.com",
        "trafficjunky.com",
        "juicyads.com",
        "ero-advertising.com",
        "monetag.com",
        "clickadu.com",
        "richaudience.com",
        "deloton.com",
        "syndication.exdynsrv.com",
        "ad-maven.com",
        "yepads.com",
        "yllix.com",
        "linkvertise.com",
        "ouo.io",
        "shorte.st",
        "bc.vc",
        "directrev.com",
        "greatdexchange.com",
        "onclicksuper.com",
        "trackvoluum.com",
        "voluumtrk.com",
        "redirectingat.com",
        "adbull.me",
        "traffichaus.com",
        "plugrush.com",
        "coinhive.com",
        "trafficstars.com",
        "bidvertiser.com",
        "adk2x.com",
        "doublepimp.com",
        "zergnet.com",
        "clck.ru",
        "adf.ly"
    )

    // 5. Gambling, Casino & Betting Networks
    val gamblingHosts: Set<String> = hashSetOf(
        "1xbet.com",
        "1x-bet.com",
        "betway.com",
        "bet365.com",
        "stake.com",
        "roobet.com",
        "melbet.com",
        "parimatch.com",
        "22bet.com",
        "dafabet.com",
        "mostbet.com",
        "betwinner.com",
        "slotv.com",
        "vulkan.com",
        "ggbet.com",
        "bwin.com",
        "unibet.com",
        "williamhill.com",
        "betfair.com",
        "paddypower.com",
        "pokerstars.com",
        "888poker.com",
        "888casino.com",
        "bc.game",
        "rollbit.com",
        "duelbits.com",
        "sportsbet.io",
        "cloudbet.com",
        "betonline.ag",
        "bovada.lv",
        "casinodot.com",
        "spinpalace.com",
        "jackpotcity.com",
        "slot88.com",
        "slotgacor.com",
        "pragmaticplay.com"
    )

    // 6. Whitelisted Authentication / OAuth Providers
    val whitelistedAuthHosts: Set<String> = hashSetOf(
        "accounts.google.com",
        "oauth2.googleapis.com",
        "appleid.apple.com",
        "login.microsoftonline.com",
        "login.live.com",
        "account.microsoft.com",
        "github.com",
        "api.github.com",
        "facebook.com",
        "www.facebook.com",
        "m.facebook.com",
        "twitter.com",
        "x.com",
        "api.twitter.com",
        "discord.com",
        "discordapp.com",
        "amazon.com",
        "api.amazon.com",
        "linkedin.com",
        "www.linkedin.com",
        "auth0.com",
        "okta.com",
        "cloudflareaccess.com"
    )

    // 7. Whitelisted Payment Gateways
    val whitelistedPaymentHosts: Set<String> = hashSetOf(
        "checkout.stripe.com",
        "js.stripe.com",
        "pay.stripe.com",
        "paypal.com",
        "www.paypal.com",
        "sandbox.paypal.com",
        "pay.google.com",
        "apple.com",
        "squareup.com",
        "square.link",
        "razorpay.com",
        "api.razorpay.com",
        "klarna.com",
        "myshopify.com",
        "shopify.com",
        "checkout.shopify.com",
        "braintreegateway.com",
        "adyen.com"
    )

    // 8. General Ad & Tracker Path Regexes
    val adPathPatterns: List<Regex> = listOf(
        Regex(".*[/?&]ad_url=.*", RegexOption.IGNORE_CASE),
        Regex(".*/ads?\\.(js|php|html|gif|png).*", RegexOption.IGNORE_CASE),
        Regex(".*/adserver/.*", RegexOption.IGNORE_CASE),
        Regex(".*/advert(s|isement|ising)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/popads.*", RegexOption.IGNORE_CASE),
        Regex(".*/banner(s)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/affiliate(s)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/track(er|ing)?\\.(js|php).*", RegexOption.IGNORE_CASE),
        Regex(".*/pixel\\.(gif|png|js).*", RegexOption.IGNORE_CASE),
        Regex(".*/telemetry(/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/beacon\\.(js|gif).*", RegexOption.IGNORE_CASE),
        Regex(".*/gtag/js.*", RegexOption.IGNORE_CASE),
        Regex(".*/fbevents\\.js.*", RegexOption.IGNORE_CASE),
        Regex(".*/analytics\\.js.*", RegexOption.IGNORE_CASE)
    )

    // 9. Video Specific Ad Requests & VAST/VMAP Patterns
    val videoAdPathPatterns: List<Regex> = listOf(
        Regex(".*/api/stats/ads.*", RegexOption.IGNORE_CASE),
        Regex(".*/api/stats/atr.*", RegexOption.IGNORE_CASE),
        Regex(".*/ptracking.*", RegexOption.IGNORE_CASE),
        Regex(".*/get_midroll_info.*", RegexOption.IGNORE_CASE),
        Regex(".*/pagead/.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]adformat=.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]ad_type=.*", RegexOption.IGNORE_CASE),
        Regex(".*[?&]ad_tag=.*", RegexOption.IGNORE_CASE),
        Regex(".*/vast(\\.xml|/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/vmap(\\.xml|/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/imasdk/.*", RegexOption.IGNORE_CASE),
        Regex(".*/gampad/ads.*", RegexOption.IGNORE_CASE),
        Regex(".*/video/ads?(/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/preroll.*", RegexOption.IGNORE_CASE),
        Regex(".*/midroll.*", RegexOption.IGNORE_CASE),
        Regex(".*/postroll.*", RegexOption.IGNORE_CASE)
    )

    // Suffix / Host Match Cache for ultra-low latency lookups
    private val hostMatchCache = ConcurrentHashMap<String, HostClassification>()

    enum class HostClassification {
        AD,
        VIDEO_AD,
        TRACKER,
        MALICIOUS,
        GAMBLING,
        AUTH_WHITELIST,
        PAYMENT_WHITELIST,
        SAFE
    }

    fun classifyHost(host: String?): HostClassification {
        if (host.isNullOrBlank()) return HostClassification.SAFE
        val clean = host.lowercase()
        return hostMatchCache.getOrPut(clean) {
            when {
                matchesHost(clean, whitelistedAuthHosts) -> HostClassification.AUTH_WHITELIST
                matchesHost(clean, whitelistedPaymentHosts) -> HostClassification.PAYMENT_WHITELIST
                matchesHost(clean, videoAdHosts) -> HostClassification.VIDEO_AD
                matchesHost(clean, adHosts) -> HostClassification.AD
                matchesHost(clean, trackerHosts) -> HostClassification.TRACKER
                matchesHost(clean, maliciousRedirectHosts) -> HostClassification.MALICIOUS
                matchesHost(clean, gamblingHosts) || containsGamblingKeywords(clean) -> HostClassification.GAMBLING
                else -> HostClassification.SAFE
            }
        }
    }

    private fun matchesHost(host: String, hostSet: Set<String>): Boolean {
        if (hostSet.contains(host)) return true
        return hostSet.any { host.endsWith(".$it") }
    }

    private fun containsGamblingKeywords(host: String): Boolean {
        val highRisk = listOf(
            "casino", "betting", "poker", "roulette", "jackpot",
            "slotgacor", "slot777", "slot88", "1xbet", "parimatch",
            "melbet", "mostbet", "betway", "claim-reward", "spin-wheel",
            "lucky-draw", "verify-human-robot", "dating-nearby", "adult-game",
            "onclickads", "popunder", "popads", "redirectingat"
        )
        return highRisk.any { host.contains(it) }
    }

    fun isLegitimateVideoPlaybackStream(url: String): Boolean {
        val lower = url.lowercase()
        // Never block legitimate video playback data streams!
        if (lower.contains("googlevideo.com/videoplayback") && !lower.contains("pagead") && !lower.contains("adformat")) {
            return true
        }
        if (lower.endsWith(".m3u8") || lower.endsWith(".mpd") || lower.contains(".m3u8?") || lower.contains(".mpd?")) {
            return true
        }
        if (lower.endsWith(".ts") || lower.endsWith(".m4s") || lower.contains("/fragment") || lower.contains("/segment")) {
            return true
        }
        return false
    }
}
