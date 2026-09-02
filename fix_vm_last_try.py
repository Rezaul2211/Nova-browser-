import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "fun handleSelectedText" in line and i > 500:
        # We found the broken handleSelectedText
        break
    new_lines.append(line)

new_content = "".join(new_lines)
new_content += """
    fun handleSelectedText(action: SelectedTextAction, selectedText: String) {
        if (selectedText.isBlank()) return
        val prompt = when (action) {
            SelectedTextAction.EXPLAIN -> "Explain the following text clearly:\\n\\n\\"$selectedText\\""
            SelectedTextAction.TRANSLATE_BANGLA -> "Translate the following text into ${settings.value.aiDefaultLanguage}:\\n\\n\\"$selectedText\\""
            SelectedTextAction.SUMMARIZE -> "Summarize this snippet:\\n\\n\\"$selectedText\\""
            SelectedTextAction.COPY -> return
            SelectedTextAction.ASK -> "Answer a question about the following text:\\n\\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_SIMPLIFY -> "Rewrite this text to be simpler:\\n\\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_SHORTEN -> "Rewrite this text to be shorter and more concise:\\n\\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_PROFESSIONAL -> "Rewrite this text in a professional tone:\\n\\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_CASUAL -> "Rewrite this text in a casual tone:\\n\\n\\"$selectedText\\""
            SelectedTextAction.REWRITE_ACADEMIC -> "Rewrite this text in an academic tone:\\n\\n\\"$selectedText\\""
        }
        askAiAboutPage(prompt)
    }
}
"""

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(new_content)

