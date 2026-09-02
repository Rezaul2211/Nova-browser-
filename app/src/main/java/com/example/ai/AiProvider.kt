package com.example.ai

enum class AiProvider(val displayName: String, val defaultModel: String, val requiresModelSelection: Boolean) {
    GEMINI("Google Gemini", "gemini-3.5-flash", false),
    OPENAI("OpenAI", "gpt-4o-mini", true),
    GROK("xAI / Grok", "grok-beta", true),
    OPENROUTER("OpenRouter", "google/gemini-flash-1.5", true),
    GROQ("Groq", "llama-3.3-70b-versatile", true);

    companion object {
        fun fromName(name: String): AiProvider {
            return entries.find { it.name == name } ?: GEMINI
        }
    }
}
