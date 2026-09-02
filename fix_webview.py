import re

with open('app/src/main/java/com/example/browser/WebViewSession.kt', 'r') as f:
    content = f.read()

# Fix onPageStarted
content = re.sub(
    r'(val currentSettings = settingsProvider\(\)\s*// Inject early cosmetic CSS hide styles, anti-redirect protection, and video ad shield\s*)if \(currentSettings\.adBlockingEnabled\) \{',
    r'\1val host = Uri.parse(cleanUrl).host ?: ""\n            val isAllowed = filterEngine.isDomainAllowed(host)\n            if (currentSettings.adBlockingEnabled && !isAllowed) {',
    content
)

# Fix onPageFinished
content = re.sub(
    r'(val currentSettings = settingsProvider\(\)\s*// Inject cosmetic CSS hide stylesheet, anti-redirect script, and video ad shield\s*)if \(currentSettings\.adBlockingEnabled\) \{',
    r'\1val host = Uri.parse(cleanUrl).host ?: ""\n            val isAllowed = filterEngine.isDomainAllowed(host)\n            if (currentSettings.adBlockingEnabled && !isAllowed) {',
    content
)

with open('app/src/main/java/com/example/browser/WebViewSession.kt', 'w') as f:
    f.write(content)
