import re

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'r') as f:
    content = f.read()

# Add imports
content = content.replace('import com.example.data.BrowserSettings', 'import com.example.data.BrowserSettings\nimport com.example.ai.AiProvider')

# Add arguments
args = """    onUpdateThemeMode: (String) -> Unit,
    onUpdateAiProvider: (AiProvider) -> Unit,
    onUpdateAiModel: (String) -> Unit,"""
content = content.replace('    onUpdateThemeMode: (String) -> Unit,', args)

# Add dropdowns
dropdowns = """
                        Spacer(modifier = Modifier.height(10.dp))
                        var providerExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = settings.aiProvider.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("AI Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                AiProvider.entries.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.displayName) },
                                        onClick = { 
                                            onUpdateAiProvider(provider)
                                            if (settings.aiModel.isBlank()) {
                                                onUpdateAiModel(provider.defaultModel)
                                            }
                                            providerExpanded = false 
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (settings.aiProvider.requiresModelSelection) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = settings.aiModel,
                                onValueChange = { onUpdateAiModel(it) },
                                label = { Text("Custom Model ID") },
                                placeholder = { Text(settings.aiProvider.defaultModel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
"""

content = content.replace('visualTransformation = PasswordVisualTransformation(),', 'visualTransformation = PasswordVisualTransformation(),') # keep it

content = re.sub(
    r'(onCheckedChange = onUpdateAiConfirm\s*\)\s*Spacer\(modifier = Modifier.height\(12.dp\)\))',
    r'\1' + dropdowns,
    content
)

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'w') as f:
    f.write(content)
