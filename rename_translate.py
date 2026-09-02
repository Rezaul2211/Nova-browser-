import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('fun translatePageToBangla()', 'fun translateCurrentPage()')
# Also update the prompt inside it
content = re.sub(
    r'Translate this webpage into Bengali \(বাংলা\)\.',
    'Translate this webpage into ${settings.value.aiDefaultLanguage}.',
    content
)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
