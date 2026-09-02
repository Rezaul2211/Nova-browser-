package com.example.privacy

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CookieController(private val context: Context) {

    private val cookieManager: CookieManager = CookieManager.getInstance()

    fun configureCookies(allowCookies: Boolean, blockThirdPartyCookies: Boolean, webView: WebView?) {
        cookieManager.setAcceptCookie(allowCookies)
        if (webView != null) {
            cookieManager.setAcceptThirdPartyCookies(webView, !blockThirdPartyCookies && allowCookies)
        }
    }

    suspend fun clearCookies(): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearSiteDataAndStorage(): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            WebStorage.getInstance().deleteAllData()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllBrowsingData(webView: WebView?): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
            webView?.clearCache(true)
            webView?.clearFormData()
            webView?.clearHistory()
            webView?.clearSslPreferences()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCookieCount(url: String): Int {
        return try {
            val cookies = cookieManager.getCookie(url) ?: return 0
            if (cookies.isBlank()) 0 else cookies.split(";").size
        } catch (e: Exception) {
            0
        }
    }
}
