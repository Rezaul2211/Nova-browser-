import re

with open('app/src/main/java/com/example/ai/GeminiAiService.kt', 'r') as f:
    content = f.read()

content = content.replace('class GeminiAiService {', 'class GeminiAiService : AiService {')
content = content.replace('suspend fun generateContent(', 'override suspend fun generateContent(\n        prompt: String,\n        customApiKey: String?,\n        systemInstruction: String?,\n        model: String?\n    ): Result<String> = withContext(Dispatchers.IO) {')
content = re.sub(r'suspend fun generateContent\([^)]+\):\s*Result<String>\s*=\s*withContext\(Dispatchers\.IO\)\s*\{', 'override suspend fun generateContent(\n        prompt: String,\n        customApiKey: String?,\n        systemInstruction: String?,\n        model: String?\n    ): Result<String> = withContext(Dispatchers.IO) {', content, count=1)

# Allow overriding model name
content = content.replace('val requestUrl = "$BASE_URL?key=$apiKey"', 'val usedModel = model ?: MODEL_NAME\n        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$usedModel:generateContent?key=$apiKey"')

with open('app/src/main/java/com/example/ai/GeminiAiService.kt', 'w') as f:
    f.write(content)
