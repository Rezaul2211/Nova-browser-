package com.example.ai

interface AiService {
    suspend fun generateContent(
        prompt: String,
        customApiKey: String? = null,
        systemInstruction: String? = null,
        model: String? = null
    ): Result<String>
}
