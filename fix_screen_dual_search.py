import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    content = f.read()

# Add a floating chip or tab row below the address bar if it's a search page.
# A simple way is to check `activeTab?.url` for common search engine query params.

dual_search_ui = """
                AddressBar(
                    tab = activeTab,
                    onNavigate = { url, isAiSearch ->
                        if (isAiSearch) {
                            viewModel.requestAiAction {
                                viewModel.openSheet(ActiveSheet.AiAssistant)
                                viewModel.askQuestion(url)
                            }
                        } else {
                            viewModel.navigateTo(url)
                        }
                    },
                    onReload = { viewModel.reloadActiveTab() },
                    onStop = { viewModel.stopActiveTab() },
                    onShieldClick = { viewModel.openSheet(ActiveSheet.PrivacyDashboard) },
                    onDesktopModeToggle = { viewModel.toggleDesktopMode() }
                )
                
                // Dual Search Tabs for Search Engine Results
                val currentUrl = activeTab?.url ?: ""
                val isSearchPage = currentUrl.contains("google.com/search") || currentUrl.contains("duckduckgo.com/?q=") || currentUrl.contains("bing.com/search")
                if (isSearchPage) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Button(
                                    onClick = { /* Already on Web Results */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("Web Results", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { 
                                        // Extract query
                                        val query = Uri.parse(currentUrl).getQueryParameter("q") ?: ""
                                        if (query.isNotBlank()) {
                                            viewModel.requestAiAction {
                                                viewModel.openSheet(ActiveSheet.AiAssistant)
                                                viewModel.askQuestion(query)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Results", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
"""

content = re.sub(
    r'AddressBar\([\s\S]*?onDesktopModeToggle = \{ viewModel\.toggleDesktopMode\(\) \}\n\s*\)',
    dual_search_ui.strip(),
    content
)

if 'android.net.Uri' not in content:
    content = content.replace('import androidx.compose.runtime.Composable', 'import android.net.Uri\nimport androidx.compose.runtime.Composable')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(content)

