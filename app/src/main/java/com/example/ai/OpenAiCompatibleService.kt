package com.example.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleService(private val baseUrl: String) : AiService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateContent(
        prompt: String,
        customApiKey: String?,
        systemInstruction: String?,
        model: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim() ?: ""
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API key is not configured for this provider. Please enter it in Settings.")
            )
        }

        val requestUrl = "$baseUrl/chat/completions"
        val usedModel = model ?: "gpt-4o-mini"
        val escapedPrompt = escapeJson(prompt)
        
        val messages = mutableListOf<String>()
        if (systemInstruction != null) {
            messages.add("""{"role": "system", "content": "${escapeJson(systemInstruction)}"}""")
        }
        messages.add("""{"role": "user", "content": "$escapedPrompt"}""")
        
        val messagesJson = messages.joinToString(",")

        val requestJson = """
            {
                "model": "$usedModel",
                "messages": [$messagesJson],
                "temperature": 0.3
            }
        """.trimIndent()

        val requestBody = requestJson.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("API Error (${response.code}): ${responseBody.take(150)}"))
            }

            val parsedText = extractTextFromResponse(responseBody)
            if (parsedText.isBlank()) {
                Result.failure(IOException("Received empty response from API."))
            } else {
                Result.success(parsedText)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage}", e))
        }
    }

    private fun extractTextFromResponse(json: String): String {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val root = mapAdapter.fromJson(json) as? Map<*, *> ?: return ""
            val choices = root["choices"] as? List<*> ?: return ""
            val firstChoice = choices.firstOrNull() as? Map<*, *> ?: return ""
            val message = firstChoice["message"] as? Map<*, *> ?: return ""
            (message["content"] as? String) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
