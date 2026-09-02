with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'r') as f:
    c = f.read()

# I deleted all instances of Ask AUREN. I will add it back inside the if block.
if 'Ask AUREN' not in c:
    c = c.replace(
        'if (!showRewriteOptions) {',
        'if (!showRewriteOptions) {\n                SelectionActionRow(icon = Icons.Default.Chat, label = "Ask AUREN", onClick = { onAction(SelectedTextAction.ASK) })'
    )
    with open('app/src/main/java/com/example/ui/components/ConfirmationDialogs.kt', 'w') as f:
        f.write(c)

