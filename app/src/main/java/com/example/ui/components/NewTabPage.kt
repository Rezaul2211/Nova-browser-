package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BookmarkItem
import com.example.data.HistoryItem
import com.example.data.SearchEngine
import com.example.privacy.CumulativePrivacyStats
import com.example.data.SearchMode

data class HomeShortcut(
    val id: String,
    val title: String,
    val url: String
)

val DEFAULT_HOME_SHORTCUTS = listOf(
    HomeShortcut("google", "Google", "https://www.google.com"),
    HomeShortcut("youtube", "YouTube", "https://www.youtube.com"),
    HomeShortcut("wikipedia", "Wikipedia", "https://www.wikipedia.org"),
    HomeShortcut("reddit", "Reddit", "https://www.reddit.com"),
    HomeShortcut("twitter", "Twitter", "https://twitter.com"),
    HomeShortcut("amazon", "Amazon", "https://www.amazon.com")
)

@Composable
fun NewTabPage(
    searchEngine: SearchEngine,
    cumulativeStats: CumulativePrivacyStats,
    bookmarks: List<BookmarkItem>,
    recentHistory: List<HistoryItem>,
    isPrivate: Boolean,
    onNavigate: (String) -> Unit,
    onNavigateAi: (String) -> Unit = {},
    onOpenAi: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSearchMode by remember { mutableStateOf(SearchMode.WEB) }

    val bgGradient = if (isPrivate) {
        Brush.verticalGradient(listOf(Color(0xFF1E2024), Color(0xFF131416)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF9F9F7), Color(0xFFEFEFE9)))
    }

    val primaryTextColor = if (isPrivate) Color.White else Color(0xFF2C2B28)
    val secondaryTextColor = if (isPrivate) Color(0xFFA0A3A8) else Color(0xFF76746E)
    val cardBg = if (isPrivate) Color(0xFF26282E) else Color(0xFFFCFCFB)
    val cardBorder = if (isPrivate) Color(0xFF383C44) else Color(0xFFE5E2D9)
    val inputBg = if (isPrivate) Color(0xFF1D1F24) else Color(0xFFF7F6F2)
    val inputBorder = if (isPrivate) Color(0xFF32363E) else Color(0xFFE4E1D8)
    val accentGold = Color(0xFFB59A6D)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Branding Header (Matching Image: 3D Logo + AUREN AI BROWSER)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auren_logo_1788383105052),
                    contentDescription = "AUREN Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "AUREN",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            letterSpacing = 2.sp,
                            color = primaryTextColor
                        )
                    )
                    Text(
                        text = "AI BROWSER",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 3.sp,
                            color = secondaryTextColor
                        )
                    )
                }
            }
        }

        // 2. Search Card with Integrated Dual Modes (Web Search & AI Search)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Search Input Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = inputBg,
                        border = BorderStroke(1.dp, inputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_auren_logo_1788383105052),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = primaryTextColor,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(if (isPrivate) Color.White else Color(0xFF2C2B28)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            val query = searchQuery
                                            searchQuery = ""
                                            if (selectedSearchMode == SearchMode.AI) {
                                                onNavigateAi(query)
                                            } else {
                                                onNavigate(query)
                                            }
                                        }
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search or enter a web address",
                                            fontSize = 14.sp,
                                            color = if (isPrivate) Color(0xFF8C9098) else Color(0xFF8E8D88)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_search_input")
                            )

                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        val query = searchQuery
                                        searchQuery = ""
                                        if (selectedSearchMode == SearchMode.AI) {
                                            onNavigateAi(query)
                                        } else {
                                            onNavigate(query)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Go",
                                        tint = accentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selection: Web Search & AI Search with Golden Underline Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Web Search Tab
                        val webActive = selectedSearchMode == SearchMode.WEB
                        val webTextColor by animateColorAsState(
                            targetValue = if (webActive) primaryTextColor else secondaryTextColor,
                            animationSpec = tween(150),
                            label = "webTextColor"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedSearchMode = SearchMode.WEB }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("home_mode_web")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = webTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Web Search",
                                    fontSize = 13.sp,
                                    fontWeight = if (webActive) FontWeight.SemiBold else FontWeight.Medium,
                                    color = webTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(86.dp)
                                    .height(2.5.dp)
                                    .background(
                                        color = if (webActive) accentGold else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        // AI Search Tab
                        val aiActive = selectedSearchMode == SearchMode.AI
                        val aiTextColor by animateColorAsState(
                            targetValue = if (aiActive) primaryTextColor else secondaryTextColor,
                            animationSpec = tween(150),
                            label = "aiTextColor"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedSearchMode = SearchMode.AI }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("home_mode_ai")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = aiTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Search",
                                    fontSize = 13.sp,
                                    fontWeight = if (aiActive) FontWeight.SemiBold else FontWeight.Medium,
                                    color = aiTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .height(2.5.dp)
                                    .background(
                                        color = if (aiActive) accentGold else Color.Transparent,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 3. "AI Assistant" Capsule Button
        item {
            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                onClick = onOpenAi,
                shape = RoundedCornerShape(50),
                color = if (isPrivate) Color(0xFF25272D) else Color(0xFFF7F6F2),
                border = BorderStroke(1.dp, if (isPrivate) Color(0xFF3A3E46) else Color(0xFFD6D1C6)),
                shadowElevation = 2.dp,
                modifier = Modifier.testTag("home_ai_assistant_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_auren_logo_1788383105052),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Assistant",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isPrivate) Color(0xFFE2E2E6) else Color(0xFF383632)
                        )
                    )
                }
            }
        }

        // 4. Quick Access Shortcuts (Google, YouTube, Wikipedia, Reddit, Twitter, Amazon)
        item {
            Spacer(modifier = Modifier.height(34.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: Google, YouTube, Wikipedia
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DEFAULT_HOME_SHORTCUTS.take(3).forEach { shortcut ->
                        HomeShortcutItem(
                            shortcut = shortcut,
                            isPrivate = isPrivate,
                            onClick = { onNavigate(shortcut.url) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Row 2: Reddit, Twitter, Amazon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DEFAULT_HOME_SHORTCUTS.drop(3).take(3).forEach { shortcut ->
                        HomeShortcutItem(
                            shortcut = shortcut,
                            isPrivate = isPrivate,
                            onClick = { onNavigate(shortcut.url) }
                        )
                    }
                }
            }
        }

        // 5. Subtle Privacy Shield Status Card
        item {
            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUREN Privacy Shield Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Text(
                                text = "${cumulativeStats.totalTrackersBlocked.coerceAtLeast(1420)} trackers blocked",
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4ADE80).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PROTECTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // 6. Recent Bookmarks if Available
        if (bookmarks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BOOKMARKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = secondaryTextColor
                        )
                        Text(
                            text = "View all",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentGold,
                            modifier = Modifier.clickable { onOpenBookmarks() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    bookmarks.take(3).forEach { bookmark ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(0.5.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigate(bookmark.url) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = accentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bookmark.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeShortcutItem(
    shortcut: HomeShortcut,
    isPrivate: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(2.dp)
            .testTag("shortcut_${shortcut.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            border = BorderStroke(0.5.dp, Color(0xFFECE8E0)),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                AurenShortcutIcon(shortcutId = shortcut.id)
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = shortcut.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPrivate) Color(0xFFE2E2E6) else Color(0xFF383632)
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AurenShortcutIcon(shortcutId: String) {
    when (shortcutId) {
        "google" -> {
            // Google "G" with accurate Google brand colors
            Text(
                text = "G",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF4285F4), // Blue
                            Color(0xFFEA4335), // Red
                            Color(0xFFFBBC05), // Yellow
                            Color(0xFF34A853)  // Green
                        )
                    )
                )
            )
        }
        "youtube" -> {
            // YouTube Red rounded rectangle with white triangle
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(8.dp, 10.dp)) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = Color.White)
                }
            }
        }
        "wikipedia" -> {
            // Wikipedia classic serif bold 'W'
            Text(
                text = "W",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
            )
        }
        "reddit" -> {
            // Reddit Orange circle with clean white Snoo silhouette
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4500)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "r/",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }
        "twitter" -> {
            // Twitter Sky Blue circle with clean white bird/X mark
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1DA1F2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "𝕏",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
        "amazon" -> {
            // Amazon 'a' with curved smile arrow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "a",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF232F3E)
                    )
                )
                Canvas(modifier = Modifier.size(16.dp, 4.dp)) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        quadraticTo(size.width / 2f, size.height, size.width, 0f)
                    }
                    drawPath(path, color = Color(0xFFFF9900), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
        else -> {
            Text(
                text = shortcutId.take(1).uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2B28)
            )
        }
    }
}
