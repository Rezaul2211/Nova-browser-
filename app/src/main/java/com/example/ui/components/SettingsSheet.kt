package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BrowserSettings
import com.example.data.SearchEngine

val SUPPORTED_AI_LANGUAGES = listOf(
    "English",
    "Bengali (বাংলা)",
    "Spanish (Español)",
    "Hindi (हिन्दी)",
    "French (Français)",
    "German (Deutsch)",
    "Arabic (العربية)",
    "Japanese (日本語)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: BrowserSettings,
    onUpdateSearchEngine: (SearchEngine) -> Unit,
    onUpdateAdBlock: (Boolean) -> Unit,
    onUpdateTrackerBlock: (Boolean) -> Unit,
    onUpdate3rdPartyCookies: (Boolean) -> Unit,
    onUpdateDoNotTrack: (Boolean) -> Unit,
    onUpdateHttpsOnly: (Boolean) -> Unit,
    onUpdateAiEnabled: (Boolean) -> Unit,
    onUpdateAiConfirm: (Boolean) -> Unit,
    onUpdateAiLanguage: (String) -> Unit,
    onUpdateCustomApiKey: (String) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onRequestClearAllData: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchEngineExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var customApiKeyInput by remember(settings.customGeminiApiKey) { mutableStateOf(settings.customGeminiApiKey) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Engine Section
            item {
                SettingsSectionHeader(title = "SEARCH ENGINE")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = searchEngineExpanded,
                            onExpandedChange = { searchEngineExpanded = !searchEngineExpanded }
                        ) {
                            OutlinedTextField(
                                value = settings.searchEngine.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Default Search Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = searchEngineExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = searchEngineExpanded,
                                onDismissRequest = { searchEngineExpanded = false }
                            ) {
                                SearchEngine.entries.forEach { engine ->
                                    DropdownMenuItem(
                                        text = { Text(engine.displayName) },
                                        onClick = {
                                            onUpdateSearchEngine(engine)
                                            searchEngineExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Privacy & Shield Section
            item {
                SettingsSectionHeader(title = "PRIVACY & PROTECTION")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsToggleRow(
                            title = "Ad Blocking",
                            subtitle = "Intercept intrusive ad banners and video ads",
                            checked = settings.adBlockingEnabled,
                            onCheckedChange = onUpdateAdBlock
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsToggleRow(
                            title = "Tracker & Analytics Guard",
                            subtitle = "Block third-party tracking scripts & telemetry",
                            checked = settings.trackerBlockingEnabled,
                            onCheckedChange = onUpdateTrackerBlock
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsToggleRow(
                            title = "Block Third-Party Cookies",
                            subtitle = "Prevent cross-site tracking via cookies",
                            checked = settings.blockThirdPartyCookies,
                            onCheckedChange = onUpdate3rdPartyCookies
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsToggleRow(
                            title = "Send 'Do Not Track'",
                            subtitle = "Signal websites not to track browsing",
                            checked = settings.doNotTrack,
                            onCheckedChange = onUpdateDoNotTrack
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsToggleRow(
                            title = "HTTPS-Only Mode",
                            subtitle = "Prefer encrypted connection whenever possible",
                            checked = settings.httpsOnlyMode,
                            onCheckedChange = onUpdateHttpsOnly
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // AI Assistant Configuration
            item {
                SettingsSectionHeader(title = "BUILT-IN AI ASSISTANT")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsToggleRow(
                            title = "Enable AI Assistant",
                            subtitle = "Webpage summarization and smart Q&A",
                            checked = settings.aiEnabled,
                            onCheckedChange = onUpdateAiEnabled
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsToggleRow(
                            title = "Ask Confirmation Before Sending",
                            subtitle = "Explicit prompt before sending page content to Gemini",
                            checked = settings.aiConfirmBeforeSend,
                            onCheckedChange = onUpdateAiConfirm
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Language selector
                        ExposedDropdownMenuBox(
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = !languageExpanded }
                        ) {
                            OutlinedTextField(
                                value = settings.aiDefaultLanguage,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Response Language") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                SUPPORTED_AI_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            onUpdateAiLanguage(lang)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Gemini API Key
                        OutlinedTextField(
                            value = customApiKeyInput,
                            onValueChange = {
                                customApiKeyInput = it
                                onUpdateCustomApiKey(it)
                            },
                            label = { Text("Custom Gemini API Key (Optional)") },
                            placeholder = { Text("Enter AI Studio API Key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Theme & Appearance
            item {
                SettingsSectionHeader(title = "APPEARANCE")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = themeExpanded,
                            onExpandedChange = { themeExpanded = !themeExpanded }
                        ) {
                            OutlinedTextField(
                                value = when (settings.darkThemeMode) {
                                    "DARK" -> "Dark Mode"
                                    "LIGHT" -> "Light Mode"
                                    else -> "System Default"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Color Theme") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default") },
                                    onClick = { onUpdateThemeMode("SYSTEM"); themeExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dark Mode") },
                                    onClick = { onUpdateThemeMode("DARK"); themeExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Light Mode") },
                                    onClick = { onUpdateThemeMode("LIGHT"); themeExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Data Management / Clear Browsing Data
            item {
                SettingsSectionHeader(title = "STORAGE & DATA")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Clear Browsing Data",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Erase all history, cookies, cached files, and local site storage.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestClearAllData,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("clear_data_settings_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Browsing Data")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // About NOVA Browser
            item {
                SettingsSectionHeader(title = "ABOUT NOVA BROWSER")

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NOVA Browser v1.0.0",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Architecture: Modern Chromium Core + Local Rule Engine + Gemini AI\nZero telemetry • No tracking • Privacy-First",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
