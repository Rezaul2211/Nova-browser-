import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

# Replace tab.evaluateJavascript with browserEngine.getSession(tabId)?.webView?.evaluateJavascript
content = content.replace('tab.evaluateJavascript(com.example.privacy.AiAdDetector.DOM_EXTRACTION_JS) { jsonResult ->', 'browserEngine.getSession(tabId)?.webView?.evaluateJavascript(com.example.privacy.AiAdDetector.DOM_EXTRACTION_JS) { jsonResult ->')
content = content.replace('tab.evaluateJavascript(injectJs) {}', 'browserEngine.getSession(tabId)?.webView?.evaluateJavascript(injectJs) {}')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)
