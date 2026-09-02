import re
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(r'geminiService\.generateContent\(\s*prompt = prompt,\s*customApiKey = settings\.value\.customGeminiApiKey\.ifBlank\s*\{\s*null\s*\}\s*\)',
                 'AiServiceFactory.createService(preferences.value.aiProvider).generateContent(prompt = prompt, customApiKey = settings.value.customGeminiApiKey.ifBlank { null }, model = preferences.value.aiModel.takeIf { it.isNotBlank() })', content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
