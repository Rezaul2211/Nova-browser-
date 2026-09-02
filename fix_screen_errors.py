import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('viewModel.askQuestion(url)', 'viewModel.askAiAboutPage(url)')
content = content.replace('viewModel.askQuestion(query)', 'viewModel.askAiAboutPage(query)')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(content)
