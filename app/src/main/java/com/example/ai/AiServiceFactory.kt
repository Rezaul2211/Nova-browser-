package com.example.ai

object AiServiceFactory {
    fun createService(provider: AiProvider): AiService {
        return when (provider) {
            AiProvider.GEMINI -> GeminiAiService()
            AiProvider.OPENAI -> OpenAiCompatibleService("https://api.openai.com/v1")
            AiProvider.GROK -> OpenAiCompatibleService("https://api.x.ai/v1")
            AiProvider.OPENROUTER -> OpenAiCompatibleService("https://openrouter.ai/api/v1")
            AiProvider.GROQ -> OpenAiCompatibleService("https://api.groq.com/openai/v1")
        }
    }
}
