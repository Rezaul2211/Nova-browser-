package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.BrowserSettings
import com.example.privacy.BlockReason
import com.example.privacy.CookieController
import com.example.privacy.CosmeticFilterEngine
import com.example.privacy.FilterEngine
import com.example.privacy.FilterRules
import com.example.privacy.NavigationDecision
import com.example.privacy.ResourceType
import com.example.privacy.VideoAdProtection
import com.example.privacy.VideoAdRequestInterceptor
import java.io.ByteArrayInputStream

class WebViewSession(
    val context: Context,
    override val tabId: String,
    override val isPrivate: Boolean,
    private val filterEngine: FilterEngine,
    private val cookieController: CookieController,
    private val settingsProvider: () -> BrowserSettings = { BrowserSettings() },
    private val onTabUpdated: (tabId: String, (BrowserTab) -> BrowserTab) -> Unit,
    private val onPageCommitted: (url: String, title: String) -> Unit,
    private val onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit,
    private val customViewCallback: CustomViewCallback? = null
) : BrowserEngineSession {

    val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private var defaultUserAgent: String = ""
    private val desktopUserAgent: String = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    @Volatile private var currentLoadedUrl: String = ""

    override val currentUrl: String get() = currentLoadedUrl
    override val canGoBack: Boolean get() = webView.canGoBack()
    override val canGoForward: Boolean get() = webView.canGoForward()

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        defaultUserAgent = settings.userAgentString

        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(true)
        settings.domStorageEnabled = !isPrivate
        settings.databaseEnabled = !isPrivate
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

        if (isPrivate) {
            settings.saveFormData = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        }

        webView.webViewClient = NovaWebViewClient()
        webView.webChromeClient = NovaWebChromeClient()
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadRequested(url, userAgent, contentDisposition, mimetype, contentLength)
        }
    }

    override fun loadUrl(url: String) {
        val formattedUrl = when {
            url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) -> url
            url.startsWith("about:", ignoreCase = true) -> url
            url.startsWith("javascript:", ignoreCase = true) -> url
            else -> "https://$url"
        }
        currentLoadedUrl = formattedUrl
        filterEngine.resetTabStats(tabId, formattedUrl)
        webView.loadUrl(formattedUrl)
    }

    override fun loadHtml(htmlData: String, baseUrl: String?) {
        val base = baseUrl ?: "about:blank"
        currentLoadedUrl = base
        webView.loadDataWithBaseURL(base, htmlData, "text/html", "UTF-8", null)
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else false
    }

    override fun goForward(): Boolean {
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else false
    }

    override fun goBackOrForward(steps: Int): Boolean {
        return if (webView.canGoBackOrForward(steps)) {
            webView.goBackOrForward(steps)
            true
        } else false
    }

    override fun getBackForwardHistory(): List<HistoryEntry> {
        val list = webView.copyBackForwardList()
        val result = mutableListOf<HistoryEntry>()
        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)
            if (item != null) {
                result.add(
                    HistoryEntry(
                        url = item.url ?: "",
                        title = item.title ?: item.url ?: "",
                        favicon = item.favicon,
                        index = i
                    )
                )
            }
        }
        return result
    }

    override fun clearHistory() {
        webView.clearHistory()
        onTabUpdated(tabId) {
            it.copy(
                canGoBack = false,
                canGoForward = false
            )
        }
    }

    override fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result ?: "")
        }
    }

    override fun setDesktopMode(enabled: Boolean) {
        webView.settings.userAgentString = if (enabled) desktopUserAgent else defaultUserAgent
        webView.settings.useWideViewPort = enabled
        onTabUpdated(tabId) { it.copy(isDesktopMode = enabled) }
        webView.reload()
    }

    override fun extractPageText(callback: (String) -> Unit) {
        val extractorJs = """
            (function() {
                try {
                    // Extract main readable content, stripping scripts, styles, and ads
                    var clone = document.body.cloneNode(true);
                    var removeTags = ['script', 'style', 'noscript', 'iframe', 'svg', 'canvas', 'footer', 'nav', 'aside', 'header'];
                    removeTags.forEach(function(tag) {
                        var elements = clone.getElementsByTagName(tag);
                        while (elements[0]) {
                            elements[0].parentNode.removeChild(elements[0]);
                        }
                    });
                    
                    var title = document.title || '';
                    var metaDesc = '';
                    var metaTag = document.querySelector('meta[name="description"]');
                    if (metaTag) metaDesc = metaTag.getAttribute('content') || '';
                    
                    var text = clone.innerText || clone.textContent || '';
                    // Clean up extra whitespace
                    text = text.replace(/\s+/g, ' ').trim();
                    if (text.length > 15000) {
                        text = text.substring(0, 15000) + '... [content truncated]';
                    }
                    
                    return JSON.stringify({
                        title: title,
                        description: metaDesc,
                        url: window.location.href,
                        content: text
                    });
                } catch(e) {
                    return JSON.stringify({ error: e.message, content: document.body.innerText || '' });
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(extractorJs) { rawJson ->
            callback(rawJson ?: "")
        }
    }

    override fun extractSelectedText(callback: (String) -> Unit) {
        val selectionJs = """
            (function() {
                return window.getSelection().toString();
            })();
        """.trimIndent()

        webView.evaluateJavascript(selectionJs) { rawSelected ->
            // JS evaluateJavascript returns JSON-encoded string (e.g. "text")
            val clean = if (rawSelected.startsWith("\"") && rawSelected.endsWith("\"") && rawSelected.length >= 2) {
                rawSelected.substring(1, rawSelected.length - 1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
            } else {
                rawSelected
            }
            callback(clean.trim())
        }
    }

    override fun destroy() {
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }

    private inner class NovaWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request == null) return false
            val targetUri = request.url ?: return false
            val currentUri = if (currentLoadedUrl.isNotBlank()) Uri.parse(currentLoadedUrl) else null
            val hasGesture = request.hasGesture()
            val isRedirect = request.isRedirect

            val decision = filterEngine.navigationProtectionEngine.evaluateNavigation(
                targetUri = targetUri,
                currentUri = currentUri,
                hasUserGesture = hasGesture,
                isRedirect = isRedirect,
                isUserDirectAction = false
            )

            return handleNavigationDecision(view, decision, targetUri)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val targetUri = Uri.parse(url)
            val currentUri = if (currentLoadedUrl.isNotBlank()) Uri.parse(currentLoadedUrl) else null

            val decision = filterEngine.navigationProtectionEngine.evaluateNavigation(
                targetUri = targetUri,
                currentUri = currentUri,
                hasUserGesture = false,
                isRedirect = true,
                isUserDirectAction = false
            )

            return handleNavigationDecision(view, decision, targetUri)
        }

        private fun handleNavigationDecision(
            view: WebView?,
            decision: NavigationDecision,
            targetUri: Uri
        ): Boolean {
            return when (decision) {
                is NavigationDecision.Allow -> {
                    // Allow normal navigation within the WebView
                    false
                }
                is NavigationDecision.Block -> {
                    // Strictly cancel navigation and keep user securely on current page
                    val stats = filterEngine.recordNavigationBlockedEvent(
                        tabId = tabId,
                        pageUrl = currentLoadedUrl,
                        targetUrl = targetUri.toString(),
                        reason = decision.reason,
                        host = decision.targetHost
                    )
                    onTabUpdated(tabId) { it.copy(privacyStats = stats) }
                    true
                }
                is NavigationDecision.ExternalIntent -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Protocol not handled on device
                    }
                    true
                }
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            if (request == null) return null

            val requestUrl = request.url ?: return null
            val pageUrlStr = currentLoadedUrl.ifBlank {
                request.requestHeaders?.get("Referer") ?: ""
            }
            val pageUri = if (pageUrlStr.isNotBlank()) Uri.parse(pageUrlStr) else null
            val currentSettings = settingsProvider()

            // 1. Dedicated Video-Ad Request Interceptor (handles VAST/VMAP empty XML, JSON ad tags, 204 beacons)
            if (currentSettings.videoAdProtectionEnabled) {
                val videoIntercept = filterEngine.videoAdRequestInterceptor.shouldIntercept(
                    request = request,
                    pageUrl = pageUri,
                    videoAdProtectionEnabled = true,
                    isDomainAllowed = { filterEngine.isDomainAllowed(it) }
                )
                if (videoIntercept is VideoAdRequestInterceptor.InterceptResult.Blocked) {
                    val pageHost = pageUri?.host ?: ""
                    val isThirdParty = pageHost.isNotBlank() && !requestUrl.host.isNullOrBlank() && !requestUrl.host!!.endsWith(pageHost)
                    val filterResult = FilterEngine.FilterResult(
                        shouldBlock = true,
                        reason = videoIntercept.reason,
                        isThirdParty = isThirdParty,
                        host = requestUrl.host ?: "",
                        resourceType = ResourceType.MEDIA
                    )
                    val stats = filterEngine.recordRequestEvent(
                        tabId = tabId,
                        pageUrl = pageUrlStr,
                        requestUrl = requestUrl.toString(),
                        result = filterResult
                    )
                    onTabUpdated(tabId) { it.copy(privacyStats = stats) }
                    return videoIntercept.response
                }
            }

            // 2. Core Request Filter (Ads, Trackers, Malicious Domains)
            val filterResult = filterEngine.shouldBlockRequest(
                requestUrl = requestUrl,
                pageUrl = pageUri,
                adBlockingEnabled = currentSettings.adBlockingEnabled,
                trackerBlockingEnabled = currentSettings.trackerBlockingEnabled,
                videoAdProtectionEnabled = currentSettings.videoAdProtectionEnabled,
                headers = request.requestHeaders
            )

            // Record request event and update tab stats reactively
            val stats = filterEngine.recordRequestEvent(
                tabId = tabId,
                pageUrl = pageUrlStr,
                requestUrl = requestUrl.toString(),
                result = filterResult
            )
            onTabUpdated(tabId) { it.copy(privacyStats = stats) }

            if (filterResult.shouldBlock) {
                // Return empty response to block the network request before download
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }

            return null
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            val cleanUrl = url ?: ""
            currentLoadedUrl = cleanUrl
            onTabUpdated(tabId) {
                it.copy(
                    url = cleanUrl,
                    isLoading = true,
                    progress = 10,
                    hasSslError = false,
                    canGoBack = webView.canGoBack(),
                    canGoForward = webView.canGoForward()
                )
            }
            val currentSettings = settingsProvider()
            // Inject early cosmetic CSS hide styles, anti-redirect protection, and video ad shield
            val host = Uri.parse(cleanUrl).host ?: ""
            val isAllowed = filterEngine.isDomainAllowed(host)
            if (currentSettings.adBlockingEnabled && !isAllowed) {
                view?.evaluateJavascript(CosmeticFilterEngine.COSMETIC_INJECTION_JS, null)
            }
            if (currentSettings.redirectProtectionEnabled) {
                view?.evaluateJavascript(FilterRules.ANTI_REDIRECT_INJECTION_JS, null)
            }
            if (currentSettings.videoAdProtectionEnabled) {
                view?.evaluateJavascript(VideoAdProtection.VIDEO_AD_SHIELD_JS, null)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            val cleanUrl = url ?: ""
            currentLoadedUrl = cleanUrl
            val title = view?.title ?: ""
            val stats = filterEngine.getPageStats(tabId)

            onTabUpdated(tabId) {
                it.copy(
                    url = cleanUrl,
                    title = if (title.isNotBlank()) title else it.title,
                    isLoading = false,
                    progress = 100,
                    canGoBack = webView.canGoBack(),
                    canGoForward = webView.canGoForward(),
                    privacyStats = stats
                )
            }

            val currentSettings = settingsProvider()
            // Inject cosmetic CSS hide stylesheet, anti-redirect script, and video ad shield
            val host = Uri.parse(cleanUrl).host ?: ""
            val isAllowed = filterEngine.isDomainAllowed(host)
            if (currentSettings.adBlockingEnabled && !isAllowed) {
                view?.evaluateJavascript(CosmeticFilterEngine.COSMETIC_INJECTION_JS, null)
            }
            if (currentSettings.redirectProtectionEnabled) {
                view?.evaluateJavascript(FilterRules.ANTI_REDIRECT_INJECTION_JS, null)
            }
            if (currentSettings.videoAdProtectionEnabled) {
                view?.evaluateJavascript(VideoAdProtection.VIDEO_AD_SHIELD_JS, null)
            }

            // Persist to history if not in private mode and not a blank page
            if (!isPrivate && cleanUrl.isNotBlank() && !cleanUrl.startsWith("about:")) {
                onPageCommitted(cleanUrl, if (title.isNotBlank()) title else cleanUrl)
            }
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            onTabUpdated(tabId) { it.copy(hasSslError = true, isLoading = false) }
            // Privacy & security first: cancel by default on SSL error
            handler?.cancel()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                onTabUpdated(tabId) {
                    it.copy(
                        isLoading = false,
                        progress = 100,
                        canGoBack = webView.canGoBack(),
                        canGoForward = webView.canGoForward()
                    )
                }
            }
        }
    }

    private inner class NovaWebChromeClient : WebChromeClient() {

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            if (resultMsg == null) return false

            // If window creation has no explicit user gesture, strictly block popunder
            if (!isUserGesture) {
                val stats = filterEngine.recordNavigationBlockedEvent(
                    tabId = tabId,
                    pageUrl = currentLoadedUrl,
                    targetUrl = "popup:unsolicited",
                    reason = BlockReason.POPUP_HIJACK,
                    host = "popunder_script"
                )
                onTabUpdated(tabId) { it.copy(privacyStats = stats) }
                return false
            }

            // Create temporary inspection view to filter new-window navigation target
            val tempWebView = WebView(context)
            tempWebView.settings.javaScriptEnabled = false
            tempWebView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                    val targetUri = req?.url
                    if (targetUri != null) {
                        val decision = filterEngine.navigationProtectionEngine.evaluateNavigation(
                            targetUri = targetUri,
                            currentUri = if (currentLoadedUrl.isNotBlank()) Uri.parse(currentLoadedUrl) else null,
                            hasUserGesture = true,
                            isRedirect = false,
                            isUserDirectAction = false
                        )
                        when (decision) {
                            is NavigationDecision.Allow -> {
                                loadUrl(targetUri.toString())
                            }
                            is NavigationDecision.Block -> {
                                val stats = filterEngine.recordNavigationBlockedEvent(
                                    tabId = tabId,
                                    pageUrl = currentLoadedUrl,
                                    targetUrl = targetUri.toString(),
                                    reason = decision.reason,
                                    host = decision.targetHost
                                )
                                onTabUpdated(tabId) { it.copy(privacyStats = stats) }
                            }
                            is NavigationDecision.ExternalIntent -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        }
                    }
                    try {
                        tempWebView.destroy()
                    } catch (e: Exception) {}
                    return true
                }
            }

            val transport = resultMsg.obj as? WebView.WebViewTransport
            transport?.webView = tempWebView
            resultMsg.sendToTarget()
            return true
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onTabUpdated(tabId) {
                it.copy(
                    progress = newProgress,
                    isLoading = newProgress < 100
                )
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) {
                onTabUpdated(tabId) { it.copy(title = title) }
            }
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
            if (icon != null) {
                onTabUpdated(tabId) { it.copy(favicon = icon) }
            }
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view != null && callback != null && customViewCallback != null) {
                customViewCallback.onShowCustomView(view, callback)
            } else {
                super.onShowCustomView(view, callback)
            }
        }

        override fun onHideCustomView() {
            customViewCallback?.onHideCustomView()
            super.onHideCustomView()
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            // Privacy default: deny dangerous permissions unless explicitly supported
            request?.deny()
        }
    }
}
