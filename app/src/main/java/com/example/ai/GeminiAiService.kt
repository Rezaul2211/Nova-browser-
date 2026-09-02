package com.example.ai

import com.example.BuildConfig
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

class GeminiAiService : AiService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val MODEL_NAME = "gemini-1.5-flash"

    override suspend fun generateContent(
        prompt: String,
        customApiKey: String?,
        systemInstruction: String?,
        model: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY") -> BuildConfig.GEMINI_API_KEY
            else -> BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings.")
            )
        }

        val usedModel = model ?: MODEL_NAME
        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$usedModel:generateContent?key=$apiKey"

        val escapedPrompt = escapeJson(prompt)
        val requestJson = if (systemInstruction != null) {
            val escapedSystem = escapeJson(systemInstruction)
            """
            {
                "systemInstruction": {
                    "parts": [{"text": "$escapedSystem"}]
                },
                "contents": [
                    {
                        "parts": [{"text": "$escapedPrompt"}]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.3,
                    "topP": 0.95
                }
            }
            """.trimIndent()
        } else {
            """
            {
                "contents": [
                    {
                        "parts": [{"text": "$escapedPrompt"}]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.3,
                    "topP": 0.95
                }
            }
            """.trimIndent()
        }

        val requestBody = requestJson.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val parsedText = extractTextFromResponse(responseBody)
            if (parsedText.isBlank()) {
                Result.failure(IOException("Received empty response from Gemini API."))
            } else {
                Result.success(parsedText)
            }
        } catch (e: Exception) {
            val message = when (e) {
                is IOException -> "Network error while contacting Gemini: ${e.localizedMessage}"
                else -> "AI error: ${e.localizedMessage}"
            }
            Result.failure(Exception(message, e))
        }
    }

    private fun extractTextFromResponse(json: String): String {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val root = mapAdapter.fromJson(json) as? Map<*, *> ?: return ""
            val candidates = root["candidates"] as? List<*> ?: return ""
            val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return ""
            val content = firstCandidate["content"] as? Map<*, *> ?: return ""
            val parts = content["parts"] as? List<*> ?: return ""
            val firstPart = parts.firstOrNull() as? Map<*, *> ?: return ""
            (firstPart["text"] as? String) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseErrorMessage(statusCode: Int, responseBody: String): String {
        return when (statusCode) {
            400 -> "Invalid request or malformed API key (400)."
            401, 403 -> "API key authorization error ($statusCode). Please verify your Gemini API key in Settings."
            429 -> "Gemini API rate limit exceeded. Please wait a moment and try again."
            500, 503 -> "Google AI service is temporarily unavailable. Please try again shortly."
            else -> "Gemini API error ($statusCode): ${responseBody.take(150)}"
        }
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
