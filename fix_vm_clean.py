import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "val prompt = when (action) {" in line and not skip:
        # Check if we are inside a function. If the previous line was "fun askAiAboutPage(userQuestion: String) {" then wait, it's not.
        # It's floating!
        # Wait, handleSelectedText declaration was deleted earlier, so it is floating.
        # Let's skip until we hit "private fun executeAiTask(userPrompt: String, systemInstruction: String) {"
        skip = True
    
    if skip and "private fun executeAiTask(" in line:
        skip = False
    
    if not skip:
        new_lines.append(line)

# Let's double check if there are any duplicate handleSelectedText definitions.
content = "".join(new_lines)
# Deduplicate handleSelectedText at the end? It's fine, the last one is correct.

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)

