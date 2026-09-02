import re

with open('app/src/main/java/com/example/ai/AiModels.kt', 'r') as f:
    content = f.read()

rewrite_options = """
    REWRITE_SIMPLIFY("Rewrite (Simplify)"),
    REWRITE_SHORTEN("Rewrite (Shorten)"),
    REWRITE_PROFESSIONAL("Rewrite (Professional)"),
    REWRITE_CASUAL("Rewrite (Casual)"),
    REWRITE_ACADEMIC("Rewrite (Academic)"),
"""
content = content.replace('REWRITE("Rewrite clearly"),', rewrite_options.strip() + ',')

with open('app/src/main/java/com/example/ai/AiModels.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm = f.read()

rewrite_vm = """
            SelectedTextAction.REWRITE_SIMPLIFY -> "Rewrite this text to be simpler:\n\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_SHORTEN -> "Rewrite this text to be shorter and more concise:\n\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_PROFESSIONAL -> "Rewrite this text in a professional tone:\n\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_CASUAL -> "Rewrite this text in a casual tone:\n\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_ACADEMIC -> "Rewrite this text in an academic tone:\n\n\\"$selectedText\\""
"""
vm = vm.replace('SelectedTextAction.REWRITE -> "Rewrite this text to be clearer and easier to read:\\n\\n\\"$selectedText\\""', rewrite_vm.strip())

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm)

with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'r') as f:
    dialog = f.read()

rewrite_dialog = """
            var showRewriteOptions by remember { mutableStateOf(false) }
            if (!showRewriteOptions) {
                SelectionActionRow(icon = Icons.Default.Chat, label = "Ask AUREN", onClick = { onAction(SelectedTextAction.ASK) })
                SelectionActionRow(icon = Icons.Default.Lightbulb, label = "Explain simply", onClick = { onAction(SelectedTextAction.EXPLAIN) })
                SelectionActionRow(icon = Icons.Default.Language, label = "Translate text", onClick = { onAction(SelectedTextAction.TRANSLATE_BANGLA) })
                SelectionActionRow(icon = Icons.Default.ShortText, label = "Summarize text", onClick = { onAction(SelectedTextAction.SUMMARIZE) })
                SelectionActionRow(icon = Icons.Default.Psychology, label = "Rewrite text...", onClick = { showRewriteOptions = true })
            } else {
                Text("Select Rewrite Style:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                SelectionActionRow(icon = Icons.Default.AutoAwesome, label = "Simplify", onClick = { onAction(SelectedTextAction.REWRITE_SIMPLIFY) })
                SelectionActionRow(icon = Icons.Default.AutoAwesome, label = "Shorten", onClick = { onAction(SelectedTextAction.REWRITE_SHORTEN) })
                SelectionActionRow(icon = Icons.Default.AutoAwesome, label = "Professional", onClick = { onAction(SelectedTextAction.REWRITE_PROFESSIONAL) })
                SelectionActionRow(icon = Icons.Default.AutoAwesome, label = "Casual", onClick = { onAction(SelectedTextAction.REWRITE_CASUAL) })
                SelectionActionRow(icon = Icons.Default.AutoAwesome, label = "Academic", onClick = { onAction(SelectedTextAction.REWRITE_ACADEMIC) })
            }
"""

dialog = re.sub(
    r'SelectionActionRow\(icon = Icons\.Default\.Lightbulb.*?SelectionActionRow\(icon = Icons\.Default\.Psychology.*?onClick = \{ onAction\(SelectedTextAction\.REWRITE\) \}\)',
    rewrite_dialog.strip(),
    dialog,
    flags=re.DOTALL
)

if 'import androidx.compose.runtime.remember' not in dialog:
    dialog = dialog.replace('import androidx.compose.runtime.Composable', 'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue')

if 'import androidx.compose.material.icons.filled.AutoAwesome' not in dialog:
    dialog = dialog.replace('import androidx.compose.material.icons.filled.Psychology', 'import androidx.compose.material.icons.filled.Psychology\nimport androidx.compose.material.icons.filled.AutoAwesome')

with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'w') as f:
    f.write(dialog)
    
