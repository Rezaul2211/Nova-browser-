import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    content = f.read()

# I want to find the DropdownMenu for moreMenuExpanded
menu_ui = """
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Summarize Page", color = MaterialTheme.colorScheme.primary) },
                                onClick = { 
                                    moreMenuExpanded = false
                                    viewModel.requestAiAction {
                                        viewModel.openSheet(ActiveSheet.AiAssistant)
                                        viewModel.summarizeCurrentPage()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text("Translate Page", color = MaterialTheme.colorScheme.primary) },
                                onClick = { 
                                    moreMenuExpanded = false
                                    viewModel.requestAiAction {
                                        viewModel.openSheet(ActiveSheet.AiAssistant)
                                        viewModel.translatePage()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
"""

content = re.sub(
    r'DropdownMenu\([\s\S]*?expanded = moreMenuExpanded,[\s\S]*?onDismissRequest = \{ moreMenuExpanded = false \},[\s\S]*?modifier = Modifier\.background\(MaterialTheme\.colorScheme\.surface\)[\s\S]*?\) \{',
    menu_ui.strip(),
    content
)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(content)

