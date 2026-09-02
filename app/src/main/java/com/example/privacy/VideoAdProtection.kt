package com.example.privacy

/**
 * Advanced Client-Side Video Ad Shield.
 * Intercepts client-side ad loaders, VAST/VMAP fetch calls, Google IMA SDK,
 * YouTube player response objects, and HTML5 video element ad hijackers.
 */
object VideoAdProtection {

    val VIDEO_AD_SHIELD_JS: String = """
        (function() {
            if (window.__nova_video_shield_v2_active) return;
            window.__nova_video_shield_v2_active = true;

            try {
                // =========================================================================
                // 1. EARLY NETWORK MONKEY-PATCHING (fetch & XMLHttpRequest)
                // =========================================================================
                var isAdUrl = function(url) {
                    if (!url || typeof url !== 'string') return false;
                    var u = url.toLowerCase();
                    if (u.indexOf('googlevideo.com/videoplayback') !== -1 && u.indexOf('adformat') === -1) {
                        return false; // legitimate video playback
                    }
                    return (
                        u.indexOf('/vast') !== -1 ||
                        u.indexOf('/vmap') !== -1 ||
                        u.indexOf('/vpaid') !== -1 ||
                        u.indexOf('imasdk.googleapis.com') !== -1 ||
                        u.indexOf('doubleclick.net/gampad') !== -1 ||
                        u.indexOf('pubads.g.doubleclick.net') !== -1 ||
                        u.indexOf('/api/stats/ads') !== -1 ||
                        u.indexOf('/api/stats/atr') !== -1 ||
                        u.indexOf('/get_midroll_info') !== -1 ||
                        u.indexOf('/youtubei/v1/player/ad_break') !== -1 ||
                        u.indexOf('ad_tag_url=') !== -1 ||
                        u.indexOf('spotxchange.com') !== -1 ||
                        u.indexOf('springserve.com') !== -1 ||
                        u.indexOf('innovid.com') !== -1 ||
                        u.indexOf('connatix.com') !== -1 ||
                        u.indexOf('aniview.com') !== -1 ||
                        u.indexOf('primis.tech') !== -1 ||
                        u.indexOf('teads.tv') !== -1 ||
                        u.indexOf('vidoomy.com') !== -1
                    );
                };

                // Patch window.fetch
                if (window.fetch) {
                    var origFetch = window.fetch;
                    window.fetch = function(resource, init) {
                        var url = (typeof resource === 'string') ? resource : (resource && resource.url ? resource.url : '');
                        if (isAdUrl(url)) {
                            var isVmap = url.indexOf('vmap') !== -1;
                            var isJson = url.indexOf('.json') !== -1 || (init && init.headers && JSON.stringify(init.headers).indexOf('json') !== -1);
                            var body = isVmap 
                                ? '<vmap:VMAP xmlns:vmap="http://www.iab.net/videosuite/vmap" version="1.0"></vmap:VMAP>'
                                : (isJson ? '{"ads":[],"adPlacements":[],"status":"ok"}' : '<VAST version="4.2"></VAST>');
                            var contentType = isJson ? 'application/json' : 'application/xml';
                            return Promise.resolve(new Response(body, {
                                status: 200,
                                statusText: 'OK',
                                headers: { 'Content-Type': contentType }
                            }));
                        }
                        return origFetch.apply(this, arguments);
                    };
                }

                // Patch XMLHttpRequest
                if (window.XMLHttpRequest) {
                    var origOpen = XMLHttpRequest.prototype.open;
                    var origSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        this.__nova_url = url;
                        return origOpen.apply(this, arguments);
                    };
                    XMLHttpRequest.prototype.send = function(data) {
                        var url = this.__nova_url || '';
                        if (isAdUrl(url)) {
                            var isVmap = url.indexOf('vmap') !== -1;
                            var isJson = url.indexOf('.json') !== -1;
                            var responseText = isVmap 
                                ? '<vmap:VMAP xmlns:vmap="http://www.iab.net/videosuite/vmap" version="1.0"></vmap:VMAP>'
                                : (isJson ? '{"ads":[],"adPlacements":[],"status":"ok"}' : '<VAST version="4.2"></VAST>');
                            Object.defineProperty(this, 'readyState', { value: 4, writable: false });
                            Object.defineProperty(this, 'status', { value: 200, writable: false });
                            Object.defineProperty(this, 'statusText', { value: 'OK', writable: false });
                            Object.defineProperty(this, 'responseText', { value: responseText, writable: false });
                            Object.defineProperty(this, 'response', { value: responseText, writable: false });
                            if (typeof this.onreadystatechange === 'function') this.onreadystatechange();
                            if (typeof this.onload === 'function') this.onload();
                            return;
                        }
                        return origSend.apply(this, arguments);
                    };
                }

                // =========================================================================
                // 2. YOUTUBE PLAYER RESPONSE SANITIZATION
                // =========================================================================
                var cleanYtPlayerResponse = function(resp) {
                    if (!resp || typeof resp !== 'object') return resp;
                    try {
                        if (resp.adPlacements) resp.adPlacements = [];
                        if (resp.playerAds) resp.playerAds = [];
                        if (resp.adSlots) resp.adSlots = [];
                        if (resp.auxiliaryUi && resp.auxiliaryUi.messageRenderers) delete resp.auxiliaryUi.messageRenderers;
                    } catch(e) {}
                    return resp;
                };

                // Intercept window.ytInitialPlayerResponse
                var _ytResp = window.ytInitialPlayerResponse;
                Object.defineProperty(window, 'ytInitialPlayerResponse', {
                    get: function() { return _ytResp; },
                    set: function(val) { _ytResp = cleanYtPlayerResponse(val); },
                    configurable: true
                });
                if (_ytResp) {
                    _ytResp = cleanYtPlayerResponse(_ytResp);
                }

                // =========================================================================
                // 3. GOOGLE IMA SDK & VIDEO AD FRAMEWORK STUBS
                // =========================================================================
                if (typeof window.google === 'undefined') window.google = {};
                if (typeof window.google.ima === 'undefined') {
                    window.google.ima = {
                        AdDisplayContainer: function(container) {
                            this.initialize = function() {};
                            this.destroy = function() {};
                        },
                        AdsLoader: function(adDisplayContainer) {
                            this._listeners = {};
                            this.requestAds = function(adsRequest) {
                                var self = this;
                                setTimeout(function() {
                                    if (self._listeners['adsManagerLoaded']) {
                                        var fakeManager = {
                                            init: function() {},
                                            start: function() {},
                                            stop: function() {},
                                            destroy: function() {},
                                            addEventListener: function() {},
                                            getCuePoints: function() { return []; },
                                            getRemainingTime: function() { return 0; }
                                        };
                                        self._listeners['adsManagerLoaded']({
                                            getAdsManager: function() { return fakeManager; }
                                        });
                                    }
                                }, 10);
                            };
                            this.contentComplete = function() {};
                            this.addEventListener = function(evt, handler) {
                                this._listeners[evt] = handler;
                            };
                        },
                        AdsRequest: function() {},
                        AdsRenderingSettings: function() {},
                        ViewMode: { NORMAL: 'normal', FULLSCREEN: 'fullscreen' },
                        AdError: { Type: { AD_LOAD: 'adLoad' } },
                        AdEvent: { Type: { ALL_ADS_COMPLETED: 'allAdsCompleted', CONTENT_RESUME_REQUESTED: 'contentResumeRequested' } }
                    };
                }

                // =========================================================================
                // 4. HTML5 VIDEO PLAYER CONTROLLER & OVERLAY NEUTRALIZATION
                // =========================================================================
                var isYouTube = window.location.hostname.indexOf('youtube.com') !== -1 || window.location.hostname.indexOf('youtu.be') !== -1;

                function neutralizeVideoAds() {
                    try {
                        // A. Universal Skip Buttons
                        var skipButtons = document.querySelectorAll(
                            '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-slot, button.ytp-ad-overlay-close-button, [id^="skip-button"], .videoAdUiSkipButton, .ad-skip-button, .video-ad-skip-button'
                        );
                        skipButtons.forEach(function(btn) {
                            if (btn && typeof btn.click === 'function') {
                                btn.click();
                            }
                        });

                        // B. YouTube Video Player Ad State
                        if (isYouTube) {
                            var player = document.querySelector('#movie_player, .html5-video-player');
                            if (player) {
                                var isAdShowing = player.classList.contains('ad-showing') || 
                                                  player.classList.contains('ad-interrupting') ||
                                                  document.querySelector('.ytp-ad-player-overlay, .ytp-ad-module') !== null;
                                if (isAdShowing) {
                                    var video = player.querySelector('video');
                                    if (video && !isNaN(video.duration) && video.duration > 0 && isFinite(video.duration)) {
                                        video.muted = true;
                                        video.playbackRate = 16.0;
                                        video.currentTime = video.duration - 0.05;
                                    }
                                }
                            }

                            // Remove YouTube Ad companion banners & promos
                            var overlays = document.querySelectorAll(
                                '.ytp-ad-overlay-container, .ytp-ad-message-container, .ytp-ad-image-overlay, ytd-display-ad-renderer, ytd-promoted-video-renderer, ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, #masthead-ad, ytm-promoted-sparkles-web-renderer'
                            );
                            overlays.forEach(function(el) {
                                el.style.setProperty('display', 'none', 'important');
                            });
                        } else {
                            // C. Generic HTML5 Ad Player Containers & Ad Iframes
                            var adContainers = document.querySelectorAll(
                                '.vjs-ima3-ad-container, .ima-ad-container, [id*="ima-ad-container"], .ad-container-overlay, .jw-ads, .fluid_ad_container'
                            );
                            adContainers.forEach(function(container) {
                                container.style.setProperty('display', 'none', 'important');
                                container.style.setProperty('pointer-events', 'none', 'important');
                            });

                            // Ensure main video player is not paused by ad scripts
                            var videos = document.querySelectorAll('video');
                            videos.forEach(function(vid) {
                                var parent = vid.parentElement;
                                var pClass = (parent && parent.className) ? String(parent.className) : '';
                                var isAdVid = pClass.indexOf('ad-') !== -1 || pClass.indexOf('ima-') !== -1 || pClass.indexOf('preroll') !== -1;
                                if (isAdVid) {
                                    if (!isNaN(vid.duration) && vid.duration > 0 && isFinite(vid.duration)) {
                                        vid.muted = true;
                                        vid.playbackRate = 16.0;
                                        vid.currentTime = vid.duration;
                                    }
                                }
                            });
                        }
                    } catch(e) {}
                }

                setInterval(neutralizeVideoAds, 250);

                var observer = new MutationObserver(function() {
                    neutralizeVideoAds();
                });

                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.body) {
                            observer.observe(document.body, { childList: true, subtree: true });
                        }
                    });
                }
            } catch (err) {
                console.error('[NOVA Shield] Video protection error:', err);
            }
        })();
    """.trimIndent()
}
