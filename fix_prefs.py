import re

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'r') as f:
    content = f.read()

# Add imports if missing
if 'com.example.ai.AiProvider' not in content:
    content = content.replace('import androidx.datastore.preferences.preferencesDataStore', 'import androidx.datastore.preferences.preferencesDataStore\nimport com.example.ai.AiProvider')

# Add properties to BrowserSettings
if 'val aiProvider: AiProvider' not in content:
    content = content.replace('val customGeminiApiKey: String = "",', 'val customGeminiApiKey: String = "",\n    val aiProvider: AiProvider = AiProvider.GEMINI,\n    val aiModel: String = "",')

# Add keys to UserPreferences
if 'KEY_AI_PROVIDER' not in content:
    content = content.replace('private val KEY_CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_key")', 'private val KEY_CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_key")\n    private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")\n    private val KEY_AI_MODEL = stringPreferencesKey("ai_model")')

# Add flow mapping
if 'aiProvider =' not in content:
    content = content.replace('customGeminiApiKey = pref[KEY_CUSTOM_GEMINI_KEY] ?: "",', 'customGeminiApiKey = pref[KEY_CUSTOM_GEMINI_KEY] ?: "",\n            aiProvider = AiProvider.fromName(pref[KEY_AI_PROVIDER] ?: AiProvider.GEMINI.name),\n            aiModel = pref[KEY_AI_MODEL] ?: "",')

# Add setters
setters = """
    suspend fun setAiProvider(provider: AiProvider) {
        context.dataStore.edit { it[KEY_AI_PROVIDER] = provider.name }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[KEY_AI_MODEL] = model.trim() }
    }
"""
if 'fun setAiProvider' not in content:
    content = content.replace('suspend fun setCustomGeminiApiKey(key: String) {', setters + '\n    suspend fun setCustomGeminiApiKey(key: String) {')

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'w') as f:
    f.write(content)
