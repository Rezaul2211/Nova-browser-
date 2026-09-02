package com.example.ai

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AI,
    SYSTEM
}

enum class AiTaskType(val label: String, val iconName: String) {
    SUMMARIZE("Summarize", "summarize"),
    EXPLAIN("Explain simply", "explain"),
    TRANSLATE_BANGLA("Translate to বাংলা", "translate"),
    EXTRACT_SPECS("Extract Key Info", "extract"),
    Q_AND_A("Ask AI", "chat")
}

enum class SelectedTextAction(val label: String) {
    EXPLAIN("Explain"),
    TRANSLATE_BANGLA("Translate to বাংলা"),
    ASK("Ask AUREN"),
    SUMMARIZE("Summarize"),
    
    REWRITE_SIMPLIFY("Simplify"),
    REWRITE_SHORTEN("Shorten"),
    REWRITE_PROFESSIONAL("Professional Tone"),
    REWRITE_CASUAL("Casual Tone"),
    REWRITE_ACADEMIC("Academic Tone"),

    COPY("Copy")
}

data class PageExtractionResult(
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val content: String = ""
)
