import re

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'r') as f:
    content = f.read()

content = content.replace('onUpdateAiModel: (String) -> Unit,', 'onUpdateAiModel: (String) -> Unit,\n    onUpdateLanguage: (String) -> Unit,')

lang_dropdown = """
                        Spacer(modifier = Modifier.height(10.dp))
                        var langExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = langExpanded,
                            onExpandedChange = { langExpanded = !langExpanded }
                        ) {
                            OutlinedTextField(
                                value = settings.preferredLanguage,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Translation Language") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = langExpanded,
                                onDismissRequest = { langExpanded = false }
                            ) {
                                SUPPORTED_AI_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = { 
                                            onUpdateLanguage(lang)
                                            langExpanded = false 
                                        }
                                    )
                                }
                            }
                        }
"""

content = content.replace('if (settings.aiProvider.requiresModelSelection) {', lang_dropdown + '\n                        if (settings.aiProvider.requiresModelSelection) {')

with open('app/src/main/java/com/example/ui/components/SettingsSheet.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    screen = f.read()

screen = screen.replace('onUpdateAiModel = { viewModel.setAiModel(it) },', 'onUpdateAiModel = { viewModel.setAiModel(it) },\n                onUpdateLanguage = { viewModel.setPreferredLanguage(it) },')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(screen)
