package com.example.ui

import android.app.Application
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.AiServiceFactory
import com.example.ai.AiService
import com.example.ai.AiProvider
import com.example.ai.JarvisAction
import com.example.ai.JarvisVoiceEngine
import com.example.ai.MessageSender
import com.example.ai.PageExtractionResult
import com.example.ai.PageExtractor
import com.example.ai.SelectedTextAction
import com.example.data.AiSearchState
import com.example.data.SearchMode
import com.example.data.SearchSource
import com.example.data.SearchUrlHelper
import com.example.browser.BrowserTab
import com.example.browser.NovaDownloadManager
import com.example.browser.TabManager
import com.example.data.BookmarkItem
import com.example.data.BrowserSettings
import com.example.data.DownloadItem
import com.example.data.HistoryItem
import com.example.data.NovaDatabase
import com.example.data.SearchEngine
import com.example.data.UserPreferences
import com.example.privacy.CookieController
import com.example.privacy.CumulativePrivacyStats
import com.example.privacy.FilterEngine
import com.example.privacy.PagePrivacyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data object Tabs : ActiveSheet
    data object PrivacyDashboard : ActiveSheet
    data object AiAssistant : ActiveSheet
    data object BookmarksHistory : ActiveSheet
    data object Downloads : ActiveSheet
    data object Settings : ActiveSheet
    data object ClearDataConfirmation : ActiveSheet
    data object AiConsentConfirmation : ActiveSheet
    data class SelectedTextMenu(val text: String) : ActiveSheet
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NovaDatabase.getInstance(application)
    private val historyDao = database.historyDao()
    private val bookmarkDao = database.bookmarkDao()
    private val downloadDao = database.downloadDao()

    val userPreferences = UserPreferences(application)
    val filterEngine = FilterEngine()
    val cookieController = CookieController(application)
    
    val downloadManager = NovaDownloadManager(
        context = application,
        downloadDao = downloadDao,
        scope = viewModelScope
    )

    val jarvisVoiceEngine = JarvisVoiceEngine(
        context = application,
        scope = viewModelScope
    )

    // Settings
    val settings: StateFlow<BrowserSettings> = userPreferences.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BrowserSettings()
        )

    // Tab Manager
    val tabManager = TabManager(
        context = application,
        filterEngine = filterEngine,
        cookieController = cookieController,
        settingsProvider = { settings.value },
        onPageCommitted = { url, title ->
            saveHistory(url, title)
        },
        onDownloadRequested = { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadManager.startDownload(url, userAgent, contentDisposition, mimeType, contentLength)
        }
    )

    val tabs: StateFlow<List<BrowserTab>> = tabManager.tabs
    val activeTabId: StateFlow<String> = tabManager.activeTabId

    val activeTab: StateFlow<BrowserTab?> = combine(tabs, activeTabId) { tabList, currentId ->
        tabList.firstOrNull { it.id == currentId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Active bottom sheet modal
    private val _activeSheet = MutableStateFlow<ActiveSheet>(ActiveSheet.None)
    val activeSheet: StateFlow<ActiveSheet> = _activeSheet.asStateFlow()

    // History and Bookmarks
    val historyList: StateFlow<List<HistoryItem>> = historyDao.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryItem>> = historyDao.getRecentHistory(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSites: StateFlow<List<HistoryItem>> = historyDao.getTopVisited(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksList: StateFlow<List<BookmarkItem>> = bookmarkDao.getBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadsList: StateFlow<List<DownloadItem>> = downloadDao.getDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCurrentPageBookmarked: StateFlow<Boolean> = activeTab.map { tab ->
        val url = tab?.url ?: ""
        if (url.isBlank() || url.startsWith("about:")) false
        else bookmarkDao.getBookmarkByUrl(url) != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Cumulative privacy stats
    val cumulativeStats: StateFlow<CumulativePrivacyStats> = filterEngine.cumulativeStats

    // AI State
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<ChatMessage>> = _aiChatMessages.asStateFlow()

    // Dual Search State
    private val _currentSearchQuery = MutableStateFlow<String?>(null)
    val currentSearchQuery: StateFlow<String?> = _currentSearchQuery.asStateFlow()

    private val _currentSearchMode = MutableStateFlow(SearchMode.WEB)
    val currentSearchMode: StateFlow<SearchMode> = _currentSearchMode.asStateFlow()

    private val _aiSearchState = MutableStateFlow(AiSearchState())
    val aiSearchState: StateFlow<AiSearchState> = _aiSearchState.asStateFlow()

    private var pendingAiAction: (() -> Unit)? = null

    init {
        // Connect Jarvis Voice Automation Engine
        jarvisVoiceEngine.getPageContext = {
            val tab = activeTab.value
            (tab?.url ?: "") to (tab?.title ?: "")
        }
        jarvisVoiceEngine.apiKeyProvider = {
            settings.value.customGeminiApiKey
        }
        jarvisVoiceEngine.onExecuteAction = { action: JarvisAction ->
            executeJarvisAction(action)
        }

        // Sync allowed domains from settings to filterEngine
        viewModelScope.launch {
            settings.map { it.allowedAdSites }.distinctUntilChanged().collect { allowed ->
                filterEngine.setAllowedDomains(allowed)
            }
        }

        // Detect search engine URLs from active tab
        viewModelScope.launch {
            activeTab.map { it?.url }.distinctUntilChanged().collect { url ->
                val detectedQuery = SearchUrlHelper.extractSearchQuery(url)
                if (detectedQuery != null) {
                    _currentSearchQuery.value = detectedQuery
                } else if (url != null && !url.startsWith("about:") && url.isNotBlank()) {
                    // Loaded a non-search page, reset search query
                    _currentSearchQuery.value = null
                    _currentSearchMode.value = SearchMode.WEB
                }
            }
        }

        // Initialize first tab
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.newTab()
        }
    }

    // Navigation & URL Handling
    fun navigateTo(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        val targetUrl = resolveUrlOrSearch(trimmed, settings.value.searchEngine)
        val activeSession = tabManager.getActiveSession()
        if (activeSession != null) {
            activeSession.loadUrl(targetUrl)
        } else {
            tabManager.newTab(targetUrl)
        }
    }

    
    fun runAiAdDetection() {
        val tab = activeTab.value ?: return
        val url = tab.url
        val tabId = tab.id
        
        viewModelScope.launch {
            tabManager.getSession(tabId)?.webView?.evaluateJavascript(com.example.privacy.AiAdDetector.DOM_EXTRACTION_JS) { jsonResult ->
                if (jsonResult.isNullOrBlank() || jsonResult == "null") return@evaluateJavascript
                
                val json = jsonResult.replace("\\\"", "\"")
                
                viewModelScope.launch {
                    val prompt = """
                        Analyze this simplified DOM tree JSON and identify CSS selectors for elements that are likely advertisements, sponsored content, or empty ad containers (e.g. ad spaces that failed to load). 
                        Return ONLY a valid JSON array of CSS selector strings. No markdown, no explanations.
                        Example: [".sponsored-box", "#ad-1234", "div[data-ad='true']"]
                        
                        DOM JSON:
                        ${json.take(8000)}
                    """.trimIndent()
                    
                    val result = com.example.ai.AiServiceFactory.createService(settings.value.aiProvider).generateContent(
                        prompt = prompt,
                        customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                        model = settings.value.aiModel.takeIf { it.isNotBlank() }
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
                                    val injectJs = """
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
                                    """.trimIndent()
                                    tabManager.getSession(tabId)?.webView?.evaluateJavascript(injectJs) {}
                                    
                                    // Log stats
                                    selectors.forEach { selector ->
                                        filterEngine.recordAiAdDetection(tabId, url, selector)
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

    fun reloadActiveTab() {
        tabManager.getActiveSession()?.reload()
    }

    fun stopActiveTab() {
        tabManager.getActiveSession()?.stopLoading()
    }

    fun goBack(): Boolean {
        return tabManager.getActiveSession()?.goBack() ?: false
    }

    fun goForward(): Boolean {
        return tabManager.getActiveSession()?.goForward() ?: false
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value ?: return
        val session = tabManager.getActiveSession() ?: return
        session.setDesktopMode(!tab.isDesktopMode)
    }

    // Tab Operations
    fun openNewTab(url: String = "", isPrivate: Boolean = false) {
        tabManager.newTab(url, isPrivate)
        _activeSheet.value = ActiveSheet.None
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }

    fun switchTab(tabId: String) {
        tabManager.switchTab(tabId)
        _activeSheet.value = ActiveSheet.None
    }

    fun restoreClosedTab() {
        tabManager.restoreClosedTab()
    }

    fun closeAllTabs(privateOnly: Boolean = false) {
        tabManager.closeAllTabs(privateOnly)
        _activeSheet.value = ActiveSheet.None
    }

    // Sheet Controls
    fun openSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = ActiveSheet.None
    }

    // Bookmarks & History
    fun toggleBookmarkCurrentPage() {
        val tab = activeTab.value ?: return
        val url = tab.url
        val title = tab.title
        if (url.isBlank() || url.startsWith("about:")) return

        viewModelScope.launch(Dispatchers.IO) {
            val existing = bookmarkDao.getBookmarkByUrl(url)
            if (existing != null) {
                bookmarkDao.deleteBookmarkByUrl(url)
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkItem(url = url, title = if (title.isNotBlank()) title else url)
                )
            }
        }
    }

    fun deleteBookmark(bookmark: BookmarkItem) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.deleteBookmarkById(bookmark.id)
        }
    }

    fun deleteHistoryItem(history: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteHistoryById(history.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearAllHistory()
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.clearAllBookmarks()
        }
    }

    private fun saveHistory(url: String, title: String) {
        val tab = activeTab.value
        if (tab?.isPrivate == true) return // Never save history for private tabs

        viewModelScope.launch(Dispatchers.IO) {
            val existing = historyDao.getHistoryByUrl(url)
            if (existing != null) {
                historyDao.updateHistory(
                    existing.copy(
                        title = if (title.isNotBlank()) title else existing.title,
                        timestamp = System.currentTimeMillis(),
                        visitCount = existing.visitCount + 1
                    )
                )
            } else {
                historyDao.insertHistory(
                    HistoryItem(url = url, title = title, timestamp = System.currentTimeMillis(), visitCount = 1)
                )
            }
        }
    }

    // Privacy & Shield Controls
    fun toggleSiteShield() {
        val tab = activeTab.value ?: return
        val host = try { Uri.parse(tab.url).host ?: "" } catch (e: Exception) { "" }
        if (host.isBlank()) return

        val isCurrentlyAllowed = filterEngine.isDomainAllowed(host)
        viewModelScope.launch {
            userPreferences.toggleSiteAdBlock(host, allow = !isCurrentlyAllowed)
            tabManager.getActiveSession()?.reload()
        }
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch {
            cookieController.clearAllBrowsingData(tabManager.getActiveSession()?.webView)
            historyDao.clearAllHistory()
            _activeSheet.value = ActiveSheet.None
        }
    }

    // Preferences Setters
    fun updateSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { userPreferences.setSearchEngine(engine) }
    }

    fun updateAdBlocking(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAdBlocking(enabled) }
    }

    fun updateTrackerBlocking(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setTrackerBlocking(enabled) }
    }

    fun updateVideoAdProtection(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setVideoAdProtection(enabled) }
    }

    fun updateRedirectProtection(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setRedirectProtection(enabled) }
    }

    fun updateBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBlockThirdPartyCookies(enabled) }
    }

    fun updateDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDoNotTrack(enabled) }
    }

    fun updateHttpsOnly(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setHttpsOnly(enabled) }
    }

    fun updateAiEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAiEnabled(enabled) }
    }

    fun updateAiConfirmBeforeSend(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAiConfirmBeforeSend(enabled) }
    }

    fun updateAiDefaultLanguage(lang: String) {
        viewModelScope.launch { userPreferences.setAiDefaultLanguage(lang) }
    }

    fun updateAiProvider(provider: AiProvider) {
        viewModelScope.launch { userPreferences.setAiProvider(provider) }
    }
    fun updateAiModel(model: String) {
        viewModelScope.launch { userPreferences.setAiModel(model) }
    }

    fun updateCustomGeminiApiKey(key: String) {
        viewModelScope.launch { userPreferences.setCustomGeminiApiKey(key) }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    // AI Assistant
    fun openAiAssistant() {
        if (!settings.value.aiEnabled) {
            _aiError.value = "AI Assistant is disabled in Settings."
        }
        _activeSheet.value = ActiveSheet.AiAssistant
    }

    fun toggleJarvisLiveMode() {
        val langCode = if (settings.value.aiDefaultLanguage.contains("Bengali", ignoreCase = true) || settings.value.aiDefaultLanguage.contains("Bangla", ignoreCase = true)) "bn-BD" else "en-US"
        jarvisVoiceEngine.toggleLiveMode(langCode)
    }

    fun openJarvisVoice() {
        toggleJarvisLiveMode()
    }

    fun requestAiAction(action: () -> Unit) {
        if (!settings.value.aiEnabled) {
            _aiError.value = "AI Assistant is disabled in Settings."
            _activeSheet.value = ActiveSheet.AiAssistant
            return
        }
        action()
    }

    fun executeJarvisAction(action: JarvisAction) {
        when (action) {
            is JarvisAction.Navigate -> {
                navigateTo(action.url)
            }
            is JarvisAction.Search -> {
                performSearch(action.query, isAiSearch = false)
            }
            is JarvisAction.Scroll -> {
                tabManager.getActiveSession()?.scrollPage(action.direction)
            }
            is JarvisAction.ClickElement -> {
                tabManager.getActiveSession()?.clickElementMatching(action.targetText)
            }
            is JarvisAction.NewTab -> {
                if (action.url != null) {
                    tabManager.newTab(action.url)
                } else {
                    tabManager.newTab()
                }
            }
            is JarvisAction.CloseTab -> {
                val current = activeTabId.value
                tabManager.closeTab(current)
            }
            is JarvisAction.RefreshPage -> {
                tabManager.getActiveSession()?.reload()
            }
            is JarvisAction.GoBack -> {
                tabManager.getActiveSession()?.goBack()
            }
            is JarvisAction.GoForward -> {
                tabManager.getActiveSession()?.goForward()
            }
            is JarvisAction.SummarizePage -> {
                summarizeCurrentPage()
            }
            is JarvisAction.TranslatePage -> {
                translateCurrentPage()
            }
            is JarvisAction.SpeakOnly -> {
                // Spoken directly by TTS
            }
        }
    }

    fun confirmAiConsent() {
        _activeSheet.value = ActiveSheet.AiAssistant
        val action = pendingAiAction
        pendingAiAction = null
        action?.invoke()
    }

    fun summarizeCurrentPage() {
        executeAiTask(
            userPrompt = "Summarize this webpage into a concise, high-level overview with key highlights.",
            systemInstruction = "You are NOVA AI, a fast privacy-first browser assistant. Summarize the webpage text concisely, using clean bullet points and clear sections. Reply in ${settings.value.aiDefaultLanguage}."
        )
    }

    fun explainPageSimply() {
        executeAiTask(
            userPrompt = "Explain this webpage in simple, plain language that anyone can understand without jargon.",
            systemInstruction = "You are NOVA AI browser assistant. Explain the core ideas of this page in simple, plain words. Reply in ${settings.value.aiDefaultLanguage}."
        )
    }

    fun translateCurrentPage() {
        executeAiTask(
            userPrompt = "Please translate the main points and summary of this webpage into natural, clear Bengali (বাংলা).",
            systemInstruction = "You are NOVA AI browser assistant. Provide a fluent, natural Bengali (বাংলা) translation and structured summary of the web content."
        )
    }

    fun extractPageSpecs() {
        executeAiTask(
            userPrompt = "Extract all key specifications, technical details, dates, pricing, or product facts from this webpage into a clean structured table or list.",
            systemInstruction = "You are NOVA AI browser assistant. Extract key specs, facts, and data points cleanly. Reply in ${settings.value.aiDefaultLanguage}."
        )
    }

    fun askAiAboutPage(userQuestion: String) {
        if (userQuestion.isBlank()) return
        addChatMessage(ChatMessage(sender = MessageSender.USER, text = userQuestion))

        executeAiTask(
            userPrompt = "User question: $userQuestion\n\nAnswer the user's question accurately using the webpage content provided.",
            systemInstruction = "You are NOVA AI browser assistant. Answer the user's question directly based on the context. Reply in ${settings.value.aiDefaultLanguage}."
        )
    }

    private fun executeAiTask(userPrompt: String, systemInstruction: String) {
        val session = tabManager.getActiveSession()
        if (session == null) {
            _aiError.value = "No active webpage to analyze."
            return
        }

        _activeSheet.value = ActiveSheet.AiAssistant
        _aiLoading.value = true
        _aiError.value = null

        session.extractPageText { rawJson ->
            val pageData = PageExtractor.parseExtractionResult(rawJson)
            val fullPrompt = buildString {
                appendLine("Webpage Title: ${pageData.title}")
                appendLine("Webpage URL: ${pageData.url}")
                if (pageData.description.isNotBlank()) {
                    appendLine("Description: ${pageData.description}")
                }
                appendLine()
                appendLine("Webpage Content:")
                appendLine(pageData.content.take(12000))
                appendLine()
                appendLine(userPrompt)
            }

            viewModelScope.launch {
                val result = AiServiceFactory.createService(settings.value.aiProvider).generateContent(
            prompt = fullPrompt,
            customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
            systemInstruction = systemInstruction,
            model = settings.value.aiModel.takeIf { it.isNotBlank() }
        )
                _aiLoading.value = false
                result.onSuccess { responseText ->
                    addChatMessage(ChatMessage(sender = MessageSender.AI, text = responseText))
                }.onFailure { error ->
                    _aiError.value = error.localizedMessage
                    addChatMessage(ChatMessage(sender = MessageSender.SYSTEM, text = "Error: ${error.localizedMessage}"))
                }
            }
        }
    }

    private fun addChatMessage(msg: ChatMessage) {
        _aiChatMessages.update { (it + msg).takeLast(30) }
    }

    fun clearAiChat() {
        _aiChatMessages.value = emptyList()
        _aiError.value = null
    }

    // Dual Search Handlers
    fun performSearch(queryOrUrl: String, isAiSearch: Boolean) {
        val trimmed = queryOrUrl.trim()
        if (trimmed.isBlank()) return

        val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            (!trimmed.contains(" ") && (
                trimmed.contains(".com") || trimmed.contains(".org") || trimmed.contains(".net") ||
                trimmed.contains(".io") || trimmed.contains(".edu") || trimmed.contains(".gov") ||
                trimmed.contains(".co") || trimmed.contains(".app") || trimmed.contains(".dev") ||
                trimmed.contains(".ai") || trimmed.contains(".me")
            ))

        if (isUrl) {
            navigateTo(trimmed)
            _currentSearchQuery.value = null
            _currentSearchMode.value = SearchMode.WEB
            return
        }

        _currentSearchQuery.value = trimmed
        _currentSearchMode.value = if (isAiSearch) SearchMode.AI else SearchMode.WEB

        // Always load web search in WebView so it's ready when user switches
        val webSearchUrl = SearchUrlHelper.buildSearchUrl(settings.value.searchEngine, trimmed)
        val activeSession = tabManager.getActiveSession()
        if (activeSession != null) {
            activeSession.loadUrl(webSearchUrl)
        } else {
            tabManager.newTab(webSearchUrl)
        }

        if (isAiSearch) {
            executeAiSearch(trimmed, forceRefresh = true)
        }
    }

    fun setSearchMode(mode: SearchMode) {
        _currentSearchMode.value = mode
        val query = _currentSearchQuery.value
        if (mode == SearchMode.AI && !query.isNullOrBlank()) {
            if (_aiSearchState.value.query != query || (_aiSearchState.value.answer == null && !_aiSearchState.value.isLoading)) {
                executeAiSearch(query)
            }
        }
    }

    fun executeAiSearch(query: String, forceRefresh: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        if (!forceRefresh && _aiSearchState.value.query == trimmed && _aiSearchState.value.answer != null) {
            return
        }

        if (!settings.value.aiEnabled) {
            _aiSearchState.value = AiSearchState(
                query = trimmed,
                isLoading = false,
                error = "AI Search is disabled in Settings. Enable AI Assistant to use AI Search.",
                providerName = settings.value.aiProvider.displayName
            )
            return
        }

        _aiSearchState.value = AiSearchState(
            query = trimmed,
            isLoading = true,
            providerName = settings.value.aiProvider.displayName,
            modelName = settings.value.aiModel.ifBlank { settings.value.aiProvider.defaultModel }
        )

        val session = tabManager.getActiveSession()
        session?.extractPageText { rawJson ->
            val pageData = PageExtractor.parseExtractionResult(rawJson)
            val isSearchEnginePage = SearchUrlHelper.isSearchEngineUrl(pageData.url)
            val webContext = if (isSearchEnginePage && pageData.content.isNotBlank()) pageData.content else ""

            val detectedSources = mutableListOf<SearchSource>()
            if (isSearchEnginePage) {
                val host = try { Uri.parse(pageData.url).host?.replace("www.", "") ?: "" } catch (e: Exception) { "" }
                if (host.isNotBlank()) {
                    detectedSources.add(
                        SearchSource(
                            title = "${settings.value.searchEngine.displayName} Results",
                            url = pageData.url,
                            domain = host
                        )
                    )
                }
            }

            viewModelScope.launch {
                val systemInstruction = """
                    You are AUREN AI Search, the intelligent search synthesis engine in AUREN Browser.
                    Your goal is to provide a concise, factual, and direct answer based on the user's search query and relevant web information.

                    Key Guidelines:
                    1. Direct & Concise: Answer the user's query directly without conversational filler ("Sure, here is...").
                    2. Synthesis: Structure your answer logically with clear markdown headings (## or ###), bullet points for key facts, and bold text for crucial terminology.
                    3. Accuracy: Strictly avoid inventing facts, statistics, or URLs. If information is uncertain or varies, state so clearly.
                    4. Freshness: For current events or time-sensitive questions, prefer the most recent known information.
                    5. Citations: Where appropriate, cite sources or domains in brackets (e.g., [Wikipedia], [Official Documentation], [BBC], [Reuters], [GitHub]) so the user can verify.
                    6. Language: Reply in ${settings.value.aiDefaultLanguage}.
                """.trimIndent()

                val prompt = buildString {
                    appendLine("User Search Query: \"$trimmed\"")
                    if (webContext.isNotBlank()) {
                        appendLine("\nRelevant Web Search Snippets & Page Context:")
                        appendLine(webContext.take(6000))
                    }
                    appendLine("\nProvide a synthesized, accurate AI search answer with key facts and source citations.")
                }

                val result = AiServiceFactory.createService(settings.value.aiProvider).generateContent(
                    prompt = prompt,
                    customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                    systemInstruction = systemInstruction,
                    model = settings.value.aiModel.takeIf { it.isNotBlank() }
                )

                result.onSuccess { answerText ->
                    val finalSources = detectedSources.toMutableList()
                    val domainRegex = Regex("""\[([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})]""")
                    domainRegex.findAll(answerText).forEach { match ->
                        val domain = match.groupValues[1]
                        if (finalSources.none { it.domain.equals(domain, ignoreCase = true) }) {
                            finalSources.add(
                                SearchSource(
                                    title = domain,
                                    url = "https://$domain",
                                    domain = domain
                                )
                            )
                        }
                    }

                    _aiSearchState.value = AiSearchState(
                        query = trimmed,
                        isLoading = false,
                        answer = answerText,
                        sources = finalSources.take(5),
                        providerName = settings.value.aiProvider.displayName,
                        modelName = settings.value.aiModel.ifBlank { settings.value.aiProvider.defaultModel }
                    )
                }.onFailure { error ->
                    _aiSearchState.value = AiSearchState(
                        query = trimmed,
                        isLoading = false,
                        error = error.localizedMessage ?: "Failed to generate AI search result. Web results remain available.",
                        providerName = settings.value.aiProvider.displayName
                    )
                }
            }
        } ?: run {
            viewModelScope.launch {
                val systemInstruction = "You are AUREN AI Search. Provide a factual, concise synthesis answering the user's search query. Reply in ${settings.value.aiDefaultLanguage}."
                val result = AiServiceFactory.createService(settings.value.aiProvider).generateContent(
                    prompt = "User Search Query: \"$trimmed\"\n\nSynthesize a clear, accurate, and structured answer for this search query with key facts and source citations.",
                    customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                    systemInstruction = systemInstruction,
                    model = settings.value.aiModel.takeIf { it.isNotBlank() }
                )
                result.onSuccess { answerText ->
                    _aiSearchState.value = AiSearchState(
                        query = trimmed,
                        isLoading = false,
                        answer = answerText,
                        providerName = settings.value.aiProvider.displayName,
                        modelName = settings.value.aiModel.ifBlank { settings.value.aiProvider.defaultModel }
                    )
                }.onFailure { error ->
                    _aiSearchState.value = AiSearchState(
                        query = trimmed,
                        isLoading = false,
                        error = error.localizedMessage ?: "Failed to generate AI search answer.",
                        providerName = settings.value.aiProvider.displayName
                    )
                }
            }
        }
    }

    fun askAiSearchFollowUp(followUpQuestion: String) {
        val trimmed = followUpQuestion.trim()
        if (trimmed.isBlank()) return
        val currentState = _aiSearchState.value
        if (currentState.answer == null) return

        val updatedFollowUps = currentState.followUps + ChatMessage(sender = MessageSender.USER, text = trimmed)
        _aiSearchState.value = currentState.copy(
            followUps = updatedFollowUps,
            isFollowUpLoading = true
        )

        viewModelScope.launch {
            val prompt = buildString {
                appendLine("Original Search Query: \"${currentState.query}\"")
                appendLine("Initial AI Search Synthesis:\n${currentState.answer}")
                appendLine()
                if (currentState.followUps.isNotEmpty()) {
                    appendLine("Follow-up Q&A History:")
                    currentState.followUps.forEach { msg ->
                        appendLine("${if (msg.sender == MessageSender.USER) "User" else "AI"}: ${msg.text}")
                    }
                    appendLine()
                }
                appendLine("Follow-up Question: $trimmed")
                appendLine("Answer the follow-up concisely and accurately in ${settings.value.aiDefaultLanguage}.")
            }

            val result = AiServiceFactory.createService(settings.value.aiProvider).generateContent(
                prompt = prompt,
                customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                systemInstruction = "You are AUREN AI Search. Answer follow-up questions accurately and concisely in ${settings.value.aiDefaultLanguage}.",
                model = settings.value.aiModel.takeIf { it.isNotBlank() }
            )

            result.onSuccess { replyText ->
                _aiSearchState.update { state ->
                    state.copy(
                        followUps = state.followUps + ChatMessage(sender = MessageSender.AI, text = replyText),
                        isFollowUpLoading = false
                    )
                }
            }.onFailure { error ->
                _aiSearchState.update { state ->
                    state.copy(
                        followUps = state.followUps + ChatMessage(sender = MessageSender.SYSTEM, text = "Error: ${error.localizedMessage}"),
                        isFollowUpLoading = false
                    )
                }
            }
        }
    }

    // Helper: URL vs Search
    companion object {
        fun resolveUrlOrSearch(query: String, searchEngine: SearchEngine): String {
            val trimmed = query.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                return trimmed
            }
            if (URLUtil.isValidUrl(trimmed) && trimmed.contains(".")) {
                return "https://$trimmed"
            }
            // Domain pattern check e.g. "github.com", "en.wikipedia.org/wiki/Kotlin"
            val domainRegex = Regex("""^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(/.*)?$""")
            if (domainRegex.matches(trimmed) && !trimmed.contains(" ")) {
                return "https://$trimmed"
            }
            // Otherwise search query
            return searchEngine.searchUrl + Uri.encode(trimmed)
        }
    }


    fun handleSelectedText(action: SelectedTextAction, selectedText: String) {
        if (selectedText.isBlank()) return

        val prompt = when (action) {
            SelectedTextAction.EXPLAIN -> "Explain the following text clearly:\n\n\"$selectedText\""
            SelectedTextAction.TRANSLATE_BANGLA -> "Translate the following text into ${settings.value.aiDefaultLanguage}:\n\n\"$selectedText\""
            SelectedTextAction.SUMMARIZE -> "Summarize this snippet:\n\n\"$selectedText\""
            SelectedTextAction.COPY -> ""
            SelectedTextAction.ASK -> "Answer a question about the following text:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE_SIMPLIFY -> "Rewrite this text to be simpler:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE_SHORTEN -> "Rewrite this text to be shorter and more concise:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE_PROFESSIONAL -> "Rewrite this text in a professional tone:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE_CASUAL -> "Rewrite this text in a casual tone:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE_ACADEMIC -> "Rewrite this text in an academic tone:\n\n\"$selectedText\""
        }
        askAiAboutPage(prompt)
    }

    override fun onCleared() {
        super.onCleared()
        jarvisVoiceEngine.destroy()
    }
}