package com.example.data

import com.example.ai.ChatMessage

enum class SearchMode {
    WEB,
    AI
}

data class SearchSource(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String = ""
)

data class AiSearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val answer: String? = null,
    val sources: List<SearchSource> = emptyList(),
    val followUps: List<ChatMessage> = emptyList(),
    val isFollowUpLoading: Boolean = false,
    val error: String? = null,
    val providerName: String = "",
    val modelName: String = ""
)
