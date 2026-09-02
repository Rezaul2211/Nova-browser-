import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

switch_fixed = """
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
"""
content = re.sub(
    r'val prompt = when \(action\) \{[\s\S]*?SelectedTextAction\.REWRITE_ACADEMIC.*?\}',
    switch_fixed.strip().replace('\\n', '\n').replace('\\"', '"'),
    content
)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)

