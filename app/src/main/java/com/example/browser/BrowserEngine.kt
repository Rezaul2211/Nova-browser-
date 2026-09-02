package com.example.browser

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import com.example.privacy.PagePrivacyStats
import kotlinx.coroutines.flow.StateFlow

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
    fun loadUrl(url: String)
    fun reload()
    fun stopLoading()
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)
    fun setDesktopMode(enabled: Boolean)
    fun extractPageText(callback: (String) -> Unit)
    fun extractSelectedText(callback: (String) -> Unit)
    fun destroy()
}

interface CustomViewCallback {
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
}
