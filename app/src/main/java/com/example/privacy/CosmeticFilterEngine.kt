package com.example.privacy

/**
 * Handles cosmetic hiding of ad frames, sponsored placeholders, blank spaces,
 * empty ad container collapsing, and neutralizes anti-adblock modals.
 */
object CosmeticFilterEngine {

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
        [aria-label*="advertisement" i],
        [aria-label*="sponsored" i],
        [data-ad-unit],
        [data-ad-slot],
        .criteo-ad,
        .native-ad,
        #ad-wrapper,
        #ad_banner,
        #bottom-ad-bar,
        .pop-under,
        .sticky-ad-footer,
        .floating-ad-unit,
        .interstitial-ad-overlay,
        ytd-display-ad-renderer,
        ytd-promoted-video-renderer,
        ytd-ad-slot-renderer,
        ytd-in-feed-ad-layout-renderer,
        #masthead-ad,
        ytm-promoted-sparkles-web-renderer {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            min-height: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
            padding: 0 !important;
            margin: 0 !important;
        }
    """.trimIndent().replace("\n", " ")

    val COSMETIC_INJECTION_JS: String = """
        (function() {
            if (window.__nova_cosmetic_installed) return;
            window.__nova_cosmetic_installed = true;

            // 1. Inject Cosmetic CSS rules
            var style = document.createElement('style');
            style.id = 'nova-adblock-styles';
            style.type = 'text/css';
            style.appendChild(document.createTextNode('$COSMETIC_CSS_HIDE_SELECTORS'));
            (document.head || document.documentElement).appendChild(style);

            // 2. Anti-Anti-Adblock Stubs
            try {
                window.canRunAds = true;
                window.isAdBlockActive = false;
                window.google_ad_client = true;
                window.adblock = false;
            } catch(e) {}

            // Helper to recursively hide empty ad containers
            function collapseEmptyAdSpace(element, maxDepth) {
                var current = element;
                var depth = 0;
                while (current && current !== document.body && depth < maxDepth) {
                    var rect = current.getBoundingClientRect();
                    // If this container is now empty or just wrapping the hidden ad
                    if (current.innerText.trim() === '' || rect.height === 0 || rect.width === 0) {
                        current.style.setProperty('display', 'none', 'important');
                        current.style.setProperty('height', '0', 'important');
                        current.style.setProperty('padding', '0', 'important');
                        current.style.setProperty('margin', '0', 'important');
                    } else {
                        break;
                    }
                    current = current.parentElement;
                    depth++;
                }
            }

            // 3. Dynamic Ad Cleanup and Blank Space Removal
            function cleanupAdsAndSpaces() {
                try {
                    // Hide empty ad blocks (where ad request was blocked)
                    var adSelectors = [
                        '.adsbygoogle', '[id^="google_ads_"]', '[id^="div-gpt-ad"]',
                        '[class*="ad-container"]', '[class*="ad-slot"]', 'ins.adsbygoogle',
                        'iframe[src*="doubleclick.net"]', 'iframe[src*="googleads"]',
                        'iframe[src*="googlesyndication.com"]', 'iframe[src*="adsystem"]'
                    ];

                    adSelectors.forEach(function(sel) {
                        document.querySelectorAll(sel).forEach(function(el) {
                            el.style.setProperty('display', 'none', 'important');
                            collapseEmptyAdSpace(el.parentElement, 3);
                        });
                    });

                    // Search for generic divs that might be reserved ad spaces
                    var allDivs = document.querySelectorAll('div');
                    allDivs.forEach(function(div) {
                        var id = (div.id || '').toLowerCase();
                        var cls = (div.className || '');
                        if (typeof cls !== 'string') {
                            if (cls.baseVal) cls = cls.baseVal;
                            else cls = '';
                        }
                        cls = cls.toLowerCase();
                        
                        var isAd = id.indexOf('ad-') === 0 || id.indexOf('-ad') !== -1 ||
                                   id.indexOf('advert') !== -1 || id.indexOf('banner') !== -1 ||
                                   cls.indexOf('ad-container') !== -1 || cls.indexOf('ad-wrapper') !== -1 ||
                                   cls.indexOf('sponsored') !== -1 || cls.indexOf('promoted') !== -1;
                        
                        if (isAd && div.innerText.trim() === '') {
                            div.style.setProperty('display', 'none', 'important');
                            div.style.setProperty('height', '0', 'important');
                            div.style.setProperty('padding', '0', 'important');
                            collapseEmptyAdSpace(div.parentElement, 2);
                        }
                    });

                    // Neutralize anti-adblock modals
                    var antiAdblockSelectors = [
                        '.fc-dialog-container', '.fc-ab-root', '.sp_message_container',
                        '.tp-modal', '.tp-backdrop', '[class*="adblock-modal"]',
                        '[class*="adblocker-backdrop"]', '[id*="adblock-overlay"]', '.modal-adblock'
                    ];
                    var foundModal = false;
                    antiAdblockSelectors.forEach(function(sel) {
                        document.querySelectorAll(sel).forEach(function(el) {
                            el.style.setProperty('display', 'none', 'important');
                            foundModal = true;
                        });
                    });

                    if (foundModal) {
                        if (document.body) {
                            document.body.style.setProperty('overflow', 'auto', 'important');
                            document.body.style.setProperty('position', 'static', 'important');
                        }
                        if (document.documentElement) {
                            document.documentElement.style.setProperty('overflow', 'auto', 'important');
                        }
                    }
                } catch(e) {}
            }

            // Run cleanup initially
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', cleanupAdsAndSpaces);
            } else {
                cleanupAdsAndSpaces();
            }
            setTimeout(cleanupAdsAndSpaces, 500);
            setTimeout(cleanupAdsAndSpaces, 1500);

            // 4. MutationObserver for lazy-loaded ads
            try {
                var observer = new MutationObserver(function(mutations) {
                    var shouldRunCleanup = false;
                    mutations.forEach(function(mutation) {
                        if (mutation.addedNodes.length > 0) {
                            shouldRunCleanup = true;
                        }
                    });
                    if (shouldRunCleanup) {
                        // Debounce the cleanup slightly to save CPU
                        if (window.__nova_cleanup_timeout) {
                            clearTimeout(window.__nova_cleanup_timeout);
                        }
                        window.__nova_cleanup_timeout = setTimeout(cleanupAdsAndSpaces, 400);
                    }
                });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            } catch(e) {}

        })();
    """.trimIndent()
}
