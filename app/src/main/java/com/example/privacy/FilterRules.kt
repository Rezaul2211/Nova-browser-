package com.example.privacy

object FilterRules {

    // Known Advertising Network Root Hostnames & Subdomains
    val AD_HOSTS: Set<String> = hashSetOf(
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
        "spotxchange.com",
        "spotx.tv",
        "tremorhub.com",
        "sovrn.com",
        "sharethrough.com",
        "triplelift.com",
        "yieldmo.com",
        "teads.tv",
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
        "smartclip.net",
        "undertone.com",
        "connatix.com",
        "conversantmedia.com",
        "dotomi.com",
        "quantserve.com",
        "yandex.ru/ads",
        "an.yandex.ru"
    )

    // Known Tracking, Analytics & Fingerprinting Domains
    val TRACKER_HOSTS: Set<String> = hashSetOf(
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

    // URL path patterns indicative of ads or tracking scripts
    val AD_PATH_PATTERNS: List<Regex> = listOf(
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

    // Cosmetic CSS selectors injected to hide placeholder frames and intrusive blocks
    val COSMETIC_CSS_HIDE_SELECTORS: String = """
        .adsbygoogle,
        [id^="google_ads_"],
        [id^="div-gpt-ad"],
        [class*="sponsored-post"],
        [class*="promoted-post"],
        [class*="ad-banner"],
        [class*="ad-container"],
        [class*="advertisement"],
        [class*="taboola-"],
        [class*="outbrain-"],
        .criteo-ad,
        .native-ad,
        #ad-wrapper,
        #ad_banner,
        #bottom-ad-bar,
        .pop-under,
        .sticky-ad-footer {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            min-height: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    """.trimIndent().replace("\n", " ")

    val COSMETIC_INJECTION_JS: String = """
        (function() {
            if (document.getElementById('nova-adblock-styles')) return;
            var style = document.createElement('style');
            style.id = 'nova-adblock-styles';
            style.type = 'text/css';
            style.appendChild(document.createTextNode('$COSMETIC_CSS_HIDE_SELECTORS'));
            (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()
}
