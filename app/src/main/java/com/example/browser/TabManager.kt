package com.example.browser

import android.content.Context
import com.example.privacy.CookieController
import com.example.privacy.FilterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class TabManager(
    private val context: Context,
    private val filterEngine: FilterEngine,
    private val cookieController: CookieController,
    private val onPageCommitted: (url: String, title: String) -> Unit,
    private val onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit,
    private val customViewCallback: CustomViewCallback? = null
) {
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val closedTabsStack = mutableListOf<BrowserTab>()
    private val sessionsMap = mutableMapOf<String, WebViewSession>()

    fun getSession(tabId: String): WebViewSession? {
        return sessionsMap[tabId]
    }

    fun getActiveSession(): WebViewSession? {
        return sessionsMap[_activeTabId.value]
    }

    fun newTab(initialUrl: String = "", isPrivate: Boolean = false): String {
        val newId = UUID.randomUUID().toString()
        val newTab = BrowserTab(
            id = newId,
            title = if (initialUrl.isBlank()) (if (isPrivate) "Private Tab" else "New Tab") else initialUrl,
            url = initialUrl,
            isPrivate = isPrivate
        )

        val session = WebViewSession(
            context = context,
            tabId = newId,
            isPrivate = isPrivate,
            filterEngine = filterEngine,
            cookieController = cookieController,
            onTabUpdated = { id, updater ->
                updateTab(id, updater)
            },
            onPageCommitted = onPageCommitted,
            onDownloadRequested = onDownloadRequested,
            customViewCallback = customViewCallback
        )

        sessionsMap[newId] = session
        _tabs.update { it + newTab }
        _activeTabId.value = newId

        if (initialUrl.isNotBlank()) {
            session.loadUrl(initialUrl)
        }

        return newId
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val tabToClose = currentTabs.firstOrNull { it.id == tabId } ?: return

        // Store non-private tabs for restore
        if (!tabToClose.isPrivate && tabToClose.url.isNotBlank()) {
            closedTabsStack.add(tabToClose)
        }

        // Clean up session
        sessionsMap[tabId]?.destroy()
        sessionsMap.remove(tabId)

        val updatedTabs = currentTabs.filter { it.id != tabId }
        _tabs.value = updatedTabs

        if (updatedTabs.isEmpty()) {
            // Open a clean default tab if all closed
            newTab()
        } else if (_activeTabId.value == tabId) {
            // Switch to previous or first tab
            val nextActive = updatedTabs.lastOrNull() ?: updatedTabs.first()
            _activeTabId.value = nextActive.id
        }
    }

    fun restoreClosedTab(): String? {
        if (closedTabsStack.isEmpty()) return null
        val restored = closedTabsStack.removeAt(closedTabsStack.size - 1)
        return newTab(restored.url, restored.isPrivate)
    }

    fun hasClosedTabs(): Boolean = closedTabsStack.isNotEmpty()

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun closeAllTabs(privateOnly: Boolean = false) {
        val currentTabs = _tabs.value
        val toRemove = if (privateOnly) currentTabs.filter { it.isPrivate } else currentTabs

        toRemove.forEach { tab ->
            sessionsMap[tab.id]?.destroy()
            sessionsMap.remove(tab.id)
        }

        val remaining = currentTabs.filterNot { toRemove.contains(it) }
        _tabs.value = remaining

        if (remaining.isEmpty()) {
            newTab()
        } else {
            _activeTabId.value = remaining.first().id
        }
    }

    private fun updateTab(tabId: String, updater: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) updater(it) else it }
        }
    }

    fun destroyAll() {
        sessionsMap.values.forEach { it.destroy() }
        sessionsMap.clear()
        _tabs.value = emptyList()
    }
}
