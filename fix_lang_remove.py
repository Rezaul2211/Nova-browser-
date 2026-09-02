import re

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'r') as f:
    content = f.read()

content = content.replace('    val preferredLanguage: String = "English",\n', '')

# We will just fix the setPreferredLanguage to use setAiDefaultLanguage which probably already exists or we create it correctly.
content = content.replace('    suspend fun setPreferredLanguage(lang: String) {\n        dataStore.edit { prefs -> prefs[Keys.PREFERRED_LANGUAGE] = lang }\n    }', '    suspend fun setAiDefaultLanguage(lang: String) {\n        context.dataStore.edit { it[KEY_AI_LANGUAGE] = lang }\n    }')

with open('app/src/main/java/com/example/data/UserPreferences.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm = f.read()
    
vm = vm.replace('setPreferredLanguage', 'setAiDefaultLanguage')
vm = vm.replace('settings.value.preferredLanguage', 'settings.value.aiDefaultLanguage')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm)

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'r') as f:
    settings = f.read()

settings = settings.replace('settings.preferredLanguage', 'settings.aiDefaultLanguage')

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'w') as f:
    f.write(settings)
