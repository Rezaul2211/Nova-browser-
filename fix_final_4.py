import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

# Fix the invalid string literal
content = content.replace('joinToString(",") { ""$it"" }', 'joinToString(",") { "\\"$it\\"" }')

# Fix return in handleSelectedText inside `when`
content = content.replace('SelectedTextAction.COPY -> return\n', '')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)

