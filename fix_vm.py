import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('import com.example.ai.GeminiAiService', 'import com.example.ai.AiServiceFactory\nimport com.example.ai.AiService\nimport com.example.ai.AiProvider')
content = content.replace('val geminiService = GeminiAiService()', '')

# Replace usages of `geminiService.generateContent` to dynamically create service
content = re.sub(r'geminiService\.generateContent\(\s*prompt = (.*?),\s*customApiKey = (.*?),\s*systemInstruction = (.*?)\s*\)', r'AiServiceFactory.createService(preferences.value.aiProvider).generateContent(\n            prompt = \1,\n            customApiKey = \2,\n            systemInstruction = \3,\n            model = preferences.value.aiModel.takeIf { it.isNotBlank() }\n        )', content)

# Sometimes it just calls `geminiService.generateContent(` without naming all args
# Let's see if we can find it exactly
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
