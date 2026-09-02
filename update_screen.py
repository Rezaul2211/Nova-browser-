import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('onUpdateThemeMode = { viewModel.setThemeMode(it) }', 'onUpdateThemeMode = { viewModel.setThemeMode(it) },\n                onUpdateAiProvider = { viewModel.setAiProvider(it) },\n                onUpdateAiModel = { viewModel.setAiModel(it) }')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm_content = f.read()

setters = """
    fun setAiProvider(provider: AiProvider) {
        viewModelScope.launch { userPreferences.setAiProvider(provider) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { userPreferences.setAiModel(model) }
    }
"""

vm_content = vm_content.replace('fun setCustomGeminiApiKey(key: String) {', setters + '\n    fun setCustomGeminiApiKey(key: String) {')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm_content)

