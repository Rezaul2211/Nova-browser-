package com.example.browser

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import com.example.privacy.PagePrivacyStats
import kotlinx.coroutines.flow.StateFlow

data class HistoryEntry(
    val url: String,
    val title: String,
    val favicon: Bitmap? = null,
    val index: Int = 0
)

sealed interface PageLoadingState {
    data object Idle : PageLoadingState
    data class Loading(val url: String, val progress: Int) : PageLoadingState
    data class Finished(val url: String, val title: String) : PageLoadingState
    data class Error(val url: String, val description: String, val errorCode: Int) : PageLoadingState
}

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "",
    val favicon: Bitmap? = null,
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val hasSslError: Boolean = false,
    val privacyStats: PagePrivacyStats = PagePrivacyStats(),
    val createdAt: Long = System.currentTimeMillis()
)

interface BrowserEngineSession {
    val tabId: String
    val isPrivate: Boolean
    val currentUrl: String
    val canGoBack: Boolean
    val canGoForward: Boolean

    // Navigation
    fun loadUrl(url: String)
    fun loadHtml(htmlData: String, baseUrl: String? = null)
    fun reload()
    fun stopLoading()

    // Back / Forward History
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun goBackOrForward(steps: Int): Boolean
    fun getBackForwardHistory(): List<HistoryEntry>
    fun clearHistory()

    // Script & Text Extraction
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)
    fun setDesktopMode(enabled: Boolean)
    fun extractPageText(callback: (String) -> Unit)
    fun extractSelectedText(callback: (String) -> Unit)

    // Lifecycle
    fun destroy()
}

interface CustomViewCallback {
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
}

