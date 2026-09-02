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

    // Known Malicious Redirect, Pop-Under, Click-Jacking & Adware Domains
    val MALICIOUS_REDIRECT_HOSTS: Set<String> = hashSetOf(
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

    // Known Gambling, Betting, Casino & Scam Networks
    val GAMBLING_BETTING_HOSTS: Set<String> = hashSetOf(
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

    // Keywords in hostnames or redirect queries that strongly indicate gambling, betting, pop-unders, or scams
    val HIGH_RISK_KEYWORDS: List<String> = listOf(
        "casino", "betting", "poker", "roulette", "jackpot",
        "slotgacor", "slot777", "slot88", "1xbet", "parimatch",
        "melbet", "mostbet", "betway", "claim-reward", "spin-wheel",
        "lucky-draw", "verify-human-robot", "dating-nearby", "adult-game",
        "onclickads", "popunder", "popads", "redirectingat"
    )

    // Whitelisted Authentication, OAuth & Identity Providers (Essential for cross-domain login flows)
    val WHITELISTED_AUTH_HOSTS: Set<String> = hashSetOf(
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

    // Whitelisted Payment Gateways and Checkout Providers
    val WHITELISTED_PAYMENT_HOSTS: Set<String> = hashSetOf(
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

    val ANTI_REDIRECT_INJECTION_JS: String = """
        (function() {
            if (window.__nova_anti_redirect_installed) return;
            window.__nova_anti_redirect_installed = true;

            try {
                // 1. Prevent malicious window.open without user gesture or targeting popunders
                var origOpen = window.open;
                window.open = function(url, target, features) {
                    if (!url || url === 'about:blank' || url === '') {
                        return origOpen ? origOpen.apply(this, arguments) : null;
                    }
                    var urlStr = String(url).toLowerCase();
                    // Block known popunder / ad parameters in window.open
                    if (urlStr.indexOf('popads') !== -1 || 
                        urlStr.indexOf('onclickads') !== -1 || 
                        urlStr.indexOf('propellerads') !== -1 || 
                        urlStr.indexOf('exoclick') !== -1 || 
                        urlStr.indexOf('deloton') !== -1 || 
                        urlStr.indexOf('clickadu') !== -1 || 
                        urlStr.indexOf('1xbet') !== -1 || 
                        urlStr.indexOf('betway') !== -1 || 
                        urlStr.indexOf('casino') !== -1 || 
                        urlStr.indexOf('slotgacor') !== -1) {
                        console.warn('[NOVA Shield] Blocked malicious popunder window.open to: ' + url);
                        return null;
                    }
                    return origOpen ? origOpen.apply(this, arguments) : null;
                };

                // 2. Prevent window.onbeforeunload trap redirects
                window.addEventListener('beforeunload', function(e) {
                    // Prevent page scripts from locking the user or triggering forced navigation
                }, true);

                // 3. Neutralize transparent clickjacking overlay divs that hijack video/article clicks
                function neutralizeOverlays() {
                    try {
                        var overlays = document.querySelectorAll('div, a, span');
                        var winW = window.innerWidth || document.documentElement.clientWidth;
                        var winH = window.innerHeight || document.documentElement.clientHeight;
                        if (winW > 0 && winH > 0) {
                            overlays.forEach(function(el) {
                                var style = window.getComputedStyle(el);
                                if (style.position === 'fixed' || style.position === 'absolute') {
                                    var zIndex = parseInt(style.zIndex, 10);
                                    if (zIndex > 1000) {
                                        var rect = el.getBoundingClientRect();
                                        // If element covers full viewport and has opacity 0 or transparent background without visible children
                                        if (rect.width >= winW * 0.95 && rect.height >= winH * 0.95 && (style.opacity === '0' || style.backgroundColor === 'rgba(0, 0, 0, 0)' || style.backgroundColor === 'transparent') && el.innerText.trim() === '') {
                                            el.style.pointerEvents = 'none';
                                            el.style.display = 'none';
                                        }
                                    }
                                }
                            });
                        }
                    } catch(e) {}
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', neutralizeOverlays);
                } else {
                    neutralizeOverlays();
                }
                setTimeout(neutralizeOverlays, 1000);
                setTimeout(neutralizeOverlays, 3000);
            } catch(e) {
                console.error('[NOVA Shield] Anti-redirect script error: ' + e);
            }
        })();
    """.trimIndent()
}
