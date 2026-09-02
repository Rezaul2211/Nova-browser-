import re

# 1. Fix UserPreferences.kt
with open('app/src/main/java/com/example/data/UserPreferences.kt', 'r') as f:
    up = f.read()

if 'setAiProvider' not in up:
    up = up.replace(
        'suspend fun setAiModel(model: String) {',
        'suspend fun setAiProvider(provider: AiProvider) {\n        context.dataStore.edit { it[KEY_AI_PROVIDER] = provider.name }\n    }\n\n    suspend fun setAiModel(model: String) {'
    )
    with open('app/src/main/java/com/example/data/UserPreferences.kt', 'w') as f:
        f.write(up)

# 2. Fix BrowserViewModel.kt
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    bvm = f.read()

if 'fun updateAiProvider' not in bvm:
    bvm = bvm.replace(
        'fun updateAiModel(model: String)',
        'fun updateAiProvider(provider: AiProvider) {\n        viewModelScope.launch { userPreferences.setAiProvider(provider) }\n    }\n    fun updateAiModel(model: String)'
    )
    # If updateAiModel wasn't there either:
    if 'fun updateAiProvider' not in bvm:
        bvm = bvm.replace(
            'fun updateCustomGeminiApiKey(key: String)',
            'fun updateAiProvider(provider: AiProvider) {\n        viewModelScope.launch { userPreferences.setAiProvider(provider) }\n    }\n    fun updateAiModel(model: String) {\n        viewModelScope.launch { userPreferences.setAiModel(model) }\n    }\n\n    fun updateCustomGeminiApiKey(key: String)'
        )

# Fix brace in BVM again just in case (the previous fix may have been wrong)
bvm = bvm.strip()
if bvm.endswith('}}}'):
    bvm = bvm[:-2]

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(bvm)

# 3. Fix BrowserScreen.kt (double commas)
with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    bs = f.read()

bs = bs.replace('onNavigate = { url -> viewModel.navigateTo(url) },,', 'onNavigate = { url -> viewModel.navigateTo(url) },')
with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(bs)

