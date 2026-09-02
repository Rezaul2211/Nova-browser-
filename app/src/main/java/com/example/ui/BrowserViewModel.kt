package com.example.ui

import android.app.Application
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiAiService
import com.example.ai.MessageSender
import com.example.ai.PageExtractionResult
import com.example.ai.PageExtractor
import com.example.ai.SelectedTextAction
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
    val geminiService = GeminiAiService()

    val downloadManager = NovaDownloadManager(
        context = application,
        downloadDao = downloadDao,
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

    private var pendingAiAction: (() -> Unit)? = null

    init {
        // Sync allowed domains from settings to filterEngine
        viewModelScope.launch {
            settings.map { it.allowedAdSites }.distinctUntilChanged().collect { allowed ->
                filterEngine.setAllowedDomains(allowed)
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

    fun updateCustomGeminiApiKey(key: String) {
        viewModelScope.launch { userPreferences.setCustomGeminiApiKey(key) }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    // AI Assistant
    fun requestAiAction(action: () -> Unit) {
        if (!settings.value.aiEnabled) {
            _aiError.value = "AI Assistant is disabled in Settings."
            _activeSheet.value = ActiveSheet.AiAssistant
            return
        }

        if (settings.value.aiConfirmBeforeSend) {
            pendingAiAction = action
            _activeSheet.value = ActiveSheet.AiConsentConfirmation
        } else {
            action()
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

    fun translatePageToBangla() {
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

    fun handleSelectedText(action: SelectedTextAction, selectedText: String) {
        if (selectedText.isBlank()) return

        val prompt = when (action) {
            SelectedTextAction.EXPLAIN -> "Explain the following text clearly:\n\n\"$selectedText\""
            SelectedTextAction.TRANSLATE_BANGLA -> "Translate the following text into Bengali (বাংলা):\n\n\"$selectedText\""
            SelectedTextAction.SUMMARIZE -> "Summarize this snippet:\n\n\"$selectedText\""
            SelectedTextAction.REWRITE -> "Rewrite this text to be clearer and easier to read:\n\n\"$selectedText\""
            SelectedTextAction.COPY -> return
        }

        _activeSheet.value = ActiveSheet.AiAssistant
        addChatMessage(ChatMessage(sender = MessageSender.USER, text = "[${action.label}]: $selectedText"))

        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            val result = geminiService.generateContent(
                prompt = prompt,
                customApiKey = settings.value.customGeminiApiKey.ifBlank { null }
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
                val result = geminiService.generateContent(
                    prompt = fullPrompt,
                    customApiKey = settings.value.customGeminiApiKey.ifBlank { null },
                    systemInstruction = systemInstruction
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
}
