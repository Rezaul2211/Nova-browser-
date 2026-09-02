package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.privacy.CookieController
import com.example.privacy.FilterEngine
import com.example.privacy.FilterRules
import java.io.ByteArrayInputStream

class WebViewSession(
    val context: Context,
    override val tabId: String,
    override val isPrivate: Boolean,
    private val filterEngine: FilterEngine,
    private val cookieController: CookieController,
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

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        defaultUserAgent = settings.userAgentString

        settings.javaScriptEnabled = true
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
            else -> "https://$url"
        }
        currentLoadedUrl = formattedUrl
        filterEngine.resetTabStats(tabId, formattedUrl)
        webView.loadUrl(formattedUrl)
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

            val filterResult = filterEngine.shouldBlockRequest(
                requestUrl = requestUrl,
                pageUrl = pageUri,
                adBlockingEnabled = true,
                trackerBlockingEnabled = true
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
            // Inject early cosmetic CSS hide styles
            view?.evaluateJavascript(FilterRules.COSMETIC_INJECTION_JS, null)
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

            // Inject cosmetic CSS hide stylesheet
            view?.evaluateJavascript(FilterRules.COSMETIC_INJECTION_JS, null)

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
            onTabUpdated(tabId) { it.copy(hasSslError = true) }
            // Privacy & security first: cancel by default on SSL error
            handler?.cancel()
        }
    }

    private inner class NovaWebChromeClient : WebChromeClient() {

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
