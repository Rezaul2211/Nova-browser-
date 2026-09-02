package com.example.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object PageExtractor {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun parseExtractionResult(rawJson: String): PageExtractionResult {
        if (rawJson.isBlank() || rawJson == "null") {
            return PageExtractionResult()
        }

        return try {
            val cleanJson = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                // If double escaped by JS engine
                moshi.adapter(String::class.java).fromJson(rawJson) ?: rawJson
            } else {
                rawJson
            }

            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanJson) as? Map<*, *> ?: return PageExtractionResult()

            PageExtractionResult(
                title = (map["title"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                url = (map["url"] as? String) ?: "",
                content = (map["content"] as? String) ?: ""
            )
        } catch (e: Exception) {
            PageExtractionResult(content = rawJson.take(5000))
        }
    }
}
