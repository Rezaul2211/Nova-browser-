import re

with open('app/src/main/java/com/example/ui/components/AddressBar.kt', 'r') as f:
    content = f.read()

# Make sure imports for AiSearch buttons are there
if 'import androidx.compose.foundation.layout.Spacer' in content:
    content = content.replace('import androidx.compose.foundation.layout.Spacer', 'import androidx.compose.foundation.layout.Spacer\nimport androidx.compose.foundation.layout.Arrangement\nimport androidx.compose.material3.TextButton\nimport androidx.compose.material.icons.filled.AutoAwesome\nimport androidx.compose.material.icons.filled.Search')

# Change onNavigate to onNavigate(String, Boolean)
content = content.replace('onNavigate: (String) -> Unit,', 'onNavigate: (String, Boolean) -> Unit,')
content = content.replace('onNavigate(inputText)', 'onNavigate(inputText, false)')

# Find the end of the isEditing block
editing_ui = """
                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { inputText = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear address",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
"""

replacement = editing_ui + """
                } else {
"""

content = content.replace(editing_ui + "                } else {", editing_ui + """
                } else {
""")

search_toggles = """
            if (isEditing && inputText.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            isEditing = false
                            focusManager.clearFocus()
                            onNavigate(inputText, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Web Search", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = { 
                            isEditing = false
                            focusManager.clearFocus()
                            onNavigate(inputText, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Search", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
"""

# Insert at the end of the Column
content = content.replace("""            // Web Loading Progress Bar""", search_toggles + """            // Web Loading Progress Bar""")

with open('app/src/main/java/com/example/ui/components/AddressBar.kt', 'w') as f:
    f.write(content)
