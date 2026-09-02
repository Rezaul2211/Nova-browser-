import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

# Add a function to scan for AI ads
ai_ads_scan = """
    fun runAiAdDetection() {
        val tab = activeTab.value ?: return
        val url = tab.url
        val tabId = tab.id
        
        viewModelScope.launch {
            tab.evaluateJavascript(com.example.privacy.AiAdDetector.DOM_EXTRACTION_JS) { jsonResult ->
                if (jsonResult.isNullOrBlank() || jsonResult == "null") return@evaluateJavascript
                
                // Unescape JSON string returned from evaluateJavascript
                val json = if (jsonResult.startsWith("\\\"") && jsonResult.endsWith("\\\"")) {
                    jsonResult.substring(1, jsonResult.length - 1).replace("\\\\\"", "\\\"")
                } else jsonResult
                
                viewModelScope.launch {
                    val prompt = \"\"\"
                        Analyze this simplified DOM tree JSON and identify CSS selectors for elements that are likely advertisements, sponsored content, or empty ad containers (e.g. ad spaces that failed to load). 
                        Return ONLY a valid JSON array of CSS selector strings. No markdown, no explanations.
                        Example: [".sponsored-box", "#ad-1234", "div[data-ad='true']"]
                        
                        DOM JSON:
                        ${json.take(8000)}
                    \"\"\".trimIndent()
                    
                    val result = com.example.ai.AiServiceFactory.createService(preferences.value.aiProvider).generateContent(
                        prompt = prompt,
                        customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                        model = preferences.value.aiModel.takeIf { it.isNotBlank() }
                    )
                    
                    result.onSuccess { responseText ->
                        try {
                            val cleanText = responseText.replace("```json", "").replace("```", "").trim()
                            val arrayMatcher = java.util.regex.Pattern.compile("\\[.*?\\]", java.util.regex.Pattern.DOTALL).matcher(cleanText)
                            if (arrayMatcher.find()) {
                                val arrayStr = arrayMatcher.group()
                                val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                                val adapter = moshi.adapter<List<String>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java))
                                val selectors = adapter.fromJson(arrayStr) ?: emptyList()
                                
                                if (selectors.isNotEmpty()) {
                                    val jsSelectors = selectors.joinToString(",") { "\"$it\"" }
                                    val injectJs = \"\"\"
                                        (function() {
                                            var selectors = [$jsSelectors];
                                            selectors.forEach(function(sel) {
                                                try {
                                                    document.querySelectorAll(sel).forEach(function(el) {
                                                        el.style.setProperty('display', 'none', 'important');
                                                        el.style.setProperty('height', '0', 'important');
                                                        el.style.setProperty('padding', '0', 'important');
                                                        el.style.setProperty('margin', '0', 'important');
                                                        // Collapse parent if empty
                                                        var parent = el.parentElement;
                                                        if (parent && parent.innerText.trim() === '') {
                                                            parent.style.setProperty('display', 'none', 'important');
                                                        }
                                                    });
                                                } catch(e) {}
                                            });
                                        })();
                                    \"\"\".trimIndent()
                                    tab.evaluateJavascript(injectJs) {}
                                    
                                    // Log stats
                                    selectors.forEach { selector ->
                                        com.example.privacy.FilterEngine.recordAiAdDetection(tabId, url, selector)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
"""

content = content.replace('fun reloadActiveTab() {', ai_ads_scan + '\n    fun reloadActiveTab() {')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
