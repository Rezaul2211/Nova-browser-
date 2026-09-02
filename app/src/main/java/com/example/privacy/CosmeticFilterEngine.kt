package com.example.privacy

/**
 * Handles cosmetic hiding of ad frames, sponsored placeholders, blank spaces,
 * and neutralizes anti-adblock modals.
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
        }
    """.trimIndent().replace("\n", " ")

    val COSMETIC_INJECTION_JS: String = """
        (function() {
            if (document.getElementById('nova-adblock-styles')) return;

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

            // 3. Dynamic Anti-Adblock Overlay Neutralization & Unblock Scrolling
            function neutralizeAntiAdblockModals() {
                try {
                    // Check for typical anti-adblock modal classes
                    var antiAdblockSelectors = [
                        '.fc-dialog-container',
                        '.fc-ab-root',
                        '.sp_message_container',
                        '.tp-modal',
                        '.tp-backdrop',
                        '[class*="adblock-modal"]',
                        '[class*="adblocker-backdrop"]',
                        '[id*="adblock-overlay"]',
                        '.modal-adblock'
                    ];

                    var foundModal = false;
                    antiAdblockSelectors.forEach(function(sel) {
                        var elements = document.querySelectorAll(sel);
                        elements.forEach(function(el) {
                            el.style.setProperty('display', 'none', 'important');
                            el.style.setProperty('visibility', 'hidden', 'important');
                            foundModal = true;
                        });
                    });

                    // If a modal was blocking the page, restore body scrolling and unblur content
                    if (foundModal) {
                        if (document.body) {
                            document.body.style.setProperty('overflow', 'auto', 'important');
                            document.body.style.setProperty('position', 'static', 'important');
                            document.body.style.setProperty('filter', 'none', 'important');
                        }
                        if (document.documentElement) {
                            document.documentElement.style.setProperty('overflow', 'auto', 'important');
                            document.documentElement.style.setProperty('filter', 'none', 'important');
                        }
                    }
                } catch(e) {}
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', neutralizeAntiAdblockModals);
            } else {
                neutralizeAntiAdblockModals();
            }
            setTimeout(neutralizeAntiAdblockModals, 1000);
            setTimeout(neutralizeAntiAdblockModals, 2500);
        })();
    """.trimIndent()
}
