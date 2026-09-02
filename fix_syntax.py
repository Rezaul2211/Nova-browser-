import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

# Fix switch syntax
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

# Unescape JSON raw string error
json_fix = """
                val json = if (jsonResult.startsWith("\\\"") && jsonResult.endsWith("\\\"")) {
                    jsonResult.substring(1, jsonResult.length - 1).replace("\\\\\"", "\\\"")
                } else jsonResult
"""
content = re.sub(
    r'val json = if \(jsonResult\.startsWith[\s\S]*?\} else jsonResult',
    json_fix.strip().replace('\\\\"', '\\"').replace('\\"', '"'),
    content
)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)

# Fix ButtonDefaults
def add_btn(path):
    with open(path, 'r') as f:
        c = f.read()
    if 'import androidx.compose.material3.ButtonDefaults' not in c:
        c = c.replace('import androidx.compose.material3.Button', 'import androidx.compose.material3.Button\nimport androidx.compose.material3.ButtonDefaults')
    with open(path, 'w') as f:
        f.write(c)

add_btn('app/src/main/java/com/example/ui/BrowserScreen.kt')
add_btn('app/src/main/java/com/example/ui/components/AddressBar.kt')

