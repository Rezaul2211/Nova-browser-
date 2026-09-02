import re

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'r') as f:
    content = f.read()

content = content.replace('suspend fun setAiDefaultLanguage(lang: String) {\n        context.dataStore.edit { it[KEY_AI_LANGUAGE] = lang }\n    }\n\n    suspend fun setAiProvider', 'suspend fun setAiProvider')

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'w') as f:
    f.write(content)
