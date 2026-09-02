import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    c = f.read()

c = c.replace('onNavigate = { url ->\n                        if (isAiSearch) {', 'onNavigate = { url, isAiSearch ->\n                        if (isAiSearch) {')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(c)

