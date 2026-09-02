package com.example.data

import android.net.Uri

object SearchUrlHelper {

    fun extractSearchQuery(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return null

            when {
                host.contains("google.") && uri.path?.contains("/search") == true -> {
                    uri.getQueryParameter("q")
                }
                host.contains("duckduckgo.com") -> {
                    uri.getQueryParameter("q")
                }
                host.contains("bing.com") && uri.path?.contains("/search") == true -> {
                    uri.getQueryParameter("q")
                }
                host.contains("brave.com") && uri.path?.contains("/search") == true -> {
                    uri.getQueryParameter("q")
                }
                host.contains("startpage.com") -> {
                    uri.getQueryParameter("query") ?: uri.getQueryParameter("q")
                }
                host.contains("ecosia.org") && uri.path?.contains("/search") == true -> {
                    uri.getQueryParameter("q")
                }
                host.contains("search.yahoo.com") -> {
                    uri.getQueryParameter("p")
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isSearchEngineUrl(url: String?): Boolean {
        return extractSearchQuery(url) != null
    }

    fun buildSearchUrl(engine: SearchEngine, query: String): String {
        return engine.searchUrl + Uri.encode(query.trim())
    }
}
