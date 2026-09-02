import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('tab.evaluateJavascript', 'tabManager.getSession(tabId)?.webView?.evaluateJavascript')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
