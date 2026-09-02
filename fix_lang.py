import re

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'r') as f:
    content = f.read()

content = content.replace('val aiModel: String = ""', 'val aiModel: String = "",\n    val preferredLanguage: String = "English"')
content = content.replace('val AI_MODEL = stringPreferencesKey("ai_model")', 'val AI_MODEL = stringPreferencesKey("ai_model")\n        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")')
content = content.replace('aiModel = prefs[Keys.AI_MODEL] ?: ""', 'aiModel = prefs[Keys.AI_MODEL] ?: "",\n                preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "English"')
content = content.replace('suspend fun setAiModel(model: String)', 'suspend fun setPreferredLanguage(lang: String) {\n        dataStore.edit { prefs -> prefs[Keys.PREFERRED_LANGUAGE] = lang }\n    }\n\n    suspend fun setAiModel(model: String)')

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm_content = f.read()

vm_content = vm_content.replace('fun setAiModel(model: String) {', 'fun setPreferredLanguage(lang: String) {\n        viewModelScope.launch { userPreferences.setPreferredLanguage(lang) }\n    }\n\n    fun setAiModel(model: String) {')
vm_content = vm_content.replace('TRANSLATE_BANGLA -> "Translate the following text into Bengali (বাংলা):\\n\\n\\"$selectedText\\""', 'TRANSLATE_BANGLA -> "Translate the following text into ${settings.value.preferredLanguage}:\\n\\n\\"$selectedText\\""')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm_content)
    
