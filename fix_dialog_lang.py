import re

with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'r') as f:
    content = f.read()

# I also need to know the preferred language inside ConfirmationDialogs, but since it's just a button label, I can just use "Translate text".
content = content.replace('SelectionActionRow(icon = Icons.Default.Language, label = "Translate to বাংলা", onClick = { onAction(SelectedTextAction.TRANSLATE_BANGLA) })', 'SelectionActionRow(icon = Icons.Default.Language, label = "Translate text", onClick = { onAction(SelectedTextAction.TRANSLATE_BANGLA) })')

# Also change the name of the Enum in AiModels.kt to TRANSLATE instead of TRANSLATE_BANGLA to be clean
# but I don't need to change the enum value name strictly, just the button label is fine for UX.

# But wait, we also have to add "Ask AUREN" to the SelectedTextAction menu.
ask_auren = """
            SelectionActionRow(icon = Icons.Default.Chat, label = "Ask AUREN", onClick = { onAction(SelectedTextAction.ASK) })
"""
content = content.replace('Spacer(modifier = Modifier.height(14.dp))', 'Spacer(modifier = Modifier.height(14.dp))\n' + ask_auren)

if 'import androidx.compose.material.icons.filled.Chat' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Language', 'import androidx.compose.material.icons.filled.Language\nimport androidx.compose.material.icons.filled.Chat')

with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'w') as f:
    f.write(content)

