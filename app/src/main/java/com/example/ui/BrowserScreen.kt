package com.example.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.AddressBar
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.AiConsentDialog
import com.example.ui.components.BookmarksHistorySheet
import com.example.ui.components.ClearDataDialog
import com.example.ui.components.DownloadsSheet
import com.example.ui.components.NewTabPage
import com.example.ui.components.PrivacyDashboardSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TabsSheet
import com.example.ui.components.TextSelectionAiSheet

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()
    val bookmarks by viewModel.bookmarksList.collectAsState()
    val history by viewModel.historyList.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val downloads by viewModel.downloadsList.collectAsState()
    val cumulativeStats by viewModel.cumulativeStats.collectAsState()
    val aiMessages by viewModel.aiChatMessages.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    var moreMenuExpanded by remember { mutableStateOf(false) }

    // Intercept Back Press
    BackHandler {
        if (activeSheet !is ActiveSheet.None) {
            viewModel.closeSheet()
        } else if (viewModel.goBack()) {
            // Handled WebView back
        } else if (tabs.size > 1) {
            viewModel.closeTab(activeTabId)
        }
    }

    val isHomeVisible = activeTab?.url.isNullOrBlank() || activeTab?.url?.startsWith("about:") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AddressBar(
                    tab = activeTab,
                    onNavigate = { viewModel.navigateTo(it) },
                    onReload = { viewModel.reloadActiveTab() },
                    onStop = { viewModel.stopActiveTab() },
                    onShieldClick = { viewModel.openSheet(ActiveSheet.PrivacyDashboard) },
                    onDesktopModeToggle = { viewModel.toggleDesktopMode() }
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(
                        onClick = { viewModel.goBack() },
                        enabled = activeTab?.canGoBack == true,
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (activeTab?.canGoBack == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { viewModel.goForward() },
                        enabled = activeTab?.canGoForward == true,
                        modifier = Modifier.testTag("nav_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (activeTab?.canGoForward == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Home
                    IconButton(
                        onClick = { viewModel.openNewTab() },
                        modifier = Modifier.testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isHomeVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Elevated Center AI Assistant Button
                    IconButton(
                        onClick = {
                            viewModel.requestAiAction {
                                viewModel.openSheet(ActiveSheet.AiAssistant)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("nav_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Tabs Switcher Button
                    IconButton(
                        onClick = { viewModel.openSheet(ActiveSheet.Tabs) },
                        modifier = Modifier.testTag("nav_tabs_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = "${tabs.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tab,
                                contentDescription = "Open Tabs",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.testTag("nav_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Page") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.toggleBookmarkCurrentPage()
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Bookmarks & History") },
                                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                onClick = {
                                    viewModel.openSheet(ActiveSheet.BookmarksHistory)
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Downloads") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    viewModel.openSheet(ActiveSheet.Downloads)
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (activeTab?.isDesktopMode == true) "Mobile Site" else "Desktop Site") },
                                leadingIcon = { Icon(Icons.Default.Phonelink, contentDescription = null) },
                                onClick = {
                                    viewModel.toggleDesktopMode()
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Privacy Dashboard") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    viewModel.openSheet(ActiveSheet.PrivacyDashboard)
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Share Page") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, activeTab?.url ?: "")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share webpage"))
                                    moreMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    viewModel.openSheet(ActiveSheet.Settings)
                                    moreMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Webview Container vs Home Page
            if (isHomeVisible) {
                NewTabPage(
                    searchEngine = settings.searchEngine,
                    cumulativeStats = cumulativeStats,
                    bookmarks = bookmarks,
                    recentHistory = recentHistory,
                    isPrivate = activeTab?.isPrivate == true,
                    onNavigate = { viewModel.navigateTo(it) },
                    onOpenAi = {
                        viewModel.requestAiAction {
                            viewModel.openSheet(ActiveSheet.AiAssistant)
                        }
                    },
                    onOpenBookmarks = { viewModel.openSheet(ActiveSheet.BookmarksHistory) },
                    onOpenHistory = { viewModel.openSheet(ActiveSheet.BookmarksHistory) }
                )
            } else {
                val activeSession = viewModel.tabManager.getActiveSession()
                if (activeSession != null) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                (activeSession.webView.parent as? ViewGroup)?.removeView(activeSession.webView)
                                addView(activeSession.webView)
                            }
                        },
                        update = { container ->
                            if (container.indexOfChild(activeSession.webView) == -1) {
                                (activeSession.webView.parent as? ViewGroup)?.removeView(activeSession.webView)
                                container.removeAllViews()
                                container.addView(activeSession.webView)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("active_webview_container")
                    )
                }
            }
        }
    }

    // Modal Sheets and Dialogs Management
    when (val currentSheet = activeSheet) {
        ActiveSheet.None -> {}

        ActiveSheet.Tabs -> {
            TabsSheet(
                tabs = tabs,
                activeTabId = activeTabId,
                onSwitchTab = { viewModel.switchTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { isPrivate -> viewModel.openNewTab(isPrivate = isPrivate) },
                onRestoreClosedTab = { viewModel.restoreClosedTab() },
                onCloseAllTabs = { isPrivateOnly -> viewModel.closeAllTabs(isPrivateOnly) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.PrivacyDashboard -> {
            PrivacyDashboardSheet(
                pageStats = activeTab?.privacyStats ?: com.example.privacy.PagePrivacyStats(),
                cumulativeStats = cumulativeStats,
                onToggleSiteShield = { viewModel.toggleSiteShield() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.AiAssistant -> {
            AiAssistantSheet(
                chatMessages = aiMessages,
                isLoading = aiLoading,
                errorMessage = aiError,
                onSummarize = { viewModel.summarizeCurrentPage() },
                onExplain = { viewModel.explainPageSimply() },
                onTranslateBangla = { viewModel.translatePageToBangla() },
                onExtractSpecs = { viewModel.extractPageSpecs() },
                onAskQuestion = { viewModel.askAiAboutPage(it) },
                onClearChat = { viewModel.clearAiChat() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.BookmarksHistory -> {
            BookmarksHistorySheet(
                bookmarks = bookmarks,
                history = history,
                onNavigate = {
                    viewModel.navigateTo(it)
                    viewModel.closeSheet()
                },
                onDeleteBookmark = { viewModel.deleteBookmark(it) },
                onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                onClearAllHistory = { viewModel.clearAllHistory() },
                onClearAllBookmarks = { viewModel.clearAllBookmarks() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.Downloads -> {
            DownloadsSheet(
                downloads = downloads,
                onOpenFile = { viewModel.downloadManager.openDownloadedFile(it) },
                onShareFile = { viewModel.downloadManager.shareDownloadedFile(it) },
                onDeleteDownload = { viewModel.downloadManager.deleteDownload(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.Settings -> {
            SettingsSheet(
                settings = settings,
                onUpdateSearchEngine = { viewModel.updateSearchEngine(it) },
                onUpdateAdBlock = { viewModel.updateAdBlocking(it) },
                onUpdateTrackerBlock = { viewModel.updateTrackerBlocking(it) },
                onUpdate3rdPartyCookies = { viewModel.updateBlockThirdPartyCookies(it) },
                onUpdateDoNotTrack = { viewModel.updateDoNotTrack(it) },
                onUpdateHttpsOnly = { viewModel.updateHttpsOnly(it) },
                onUpdateAiEnabled = { viewModel.updateAiEnabled(it) },
                onUpdateAiConfirm = { viewModel.updateAiConfirmBeforeSend(it) },
                onUpdateAiLanguage = { viewModel.updateAiDefaultLanguage(it) },
                onUpdateCustomApiKey = { viewModel.updateCustomGeminiApiKey(it) },
                onUpdateThemeMode = { viewModel.updateThemeMode(it) },
                onRequestClearAllData = { viewModel.openSheet(ActiveSheet.ClearDataConfirmation) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.ClearDataConfirmation -> {
            ClearDataDialog(
                onConfirm = { viewModel.clearAllBrowsingData() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        ActiveSheet.AiConsentConfirmation -> {
            AiConsentDialog(
                onConfirm = { viewModel.confirmAiConsent() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.SelectedTextMenu -> {
            TextSelectionAiSheet(
                selectedText = currentSheet.text,
                onAction = { action ->
                    viewModel.handleSelectedText(action, currentSheet.text)
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }
    }
}
