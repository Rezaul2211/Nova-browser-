import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    c = f.read()

# Replace NewTabPage call block
new_tab_page_fix = """
                    isPrivate = activeTab?.isPrivate == true,
                    onNavigate = { url -> viewModel.navigateTo(url) }
                )
"""

c = re.sub(
    r'isPrivate = activeTab\?\.isPrivate == true,\s*onNavigate = \{ url, isAi ->[\s\S]*?\} else \{[\s\S]*?\}[\s\n]*\}[\s\n]*\)',
    new_tab_page_fix.strip(),
    c
)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(c)
