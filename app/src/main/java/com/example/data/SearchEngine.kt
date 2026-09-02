package com.example.data

enum class SearchEngine(
    val displayName: String,
    val searchUrl: String,
    val homeUrl: String,
    val iconDomain: String
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        searchUrl = "https://duckduckgo.com/?q=",
        homeUrl = "https://duckduckgo.com",
        iconDomain = "duckduckgo.com"
    ),
    BRAVE(
        displayName = "Brave Search",
        searchUrl = "https://search.brave.com/search?q=",
        homeUrl = "https://search.brave.com",
        iconDomain = "brave.com"
    ),
    STARTPAGE(
        displayName = "Startpage",
        searchUrl = "https://www.startpage.com/sp/search?query=",
        homeUrl = "https://www.startpage.com",
        iconDomain = "startpage.com"
    ),
    GOOGLE(
        displayName = "Google",
        searchUrl = "https://www.google.com/search?q=",
        homeUrl = "https://www.google.com",
        iconDomain = "google.com"
    ),
    BING(
        displayName = "Bing",
        searchUrl = "https://www.bing.com/search?q=",
        homeUrl = "https://www.bing.com",
        iconDomain = "bing.com"
    );

    companion object {
        fun fromName(name: String): SearchEngine {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DUCKDUCKGO
        }
    }
}
