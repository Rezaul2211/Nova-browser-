import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    content = f.read()

new_tab_page_fix = """
                    isPrivate = activeTab?.isPrivate == true,
                    onNavigate = { url -> viewModel.navigateTo(url) }
                )
"""

content = re.sub(
    r'isPrivate = activeTab\?\.isPrivate == true,\s*onNavigate = \{ url, isAiSearch ->[\s\S]*?\} else \{[\s\S]*?\}[\s\n]*\}[\s\n]*\)',
    new_tab_page_fix.strip(),
    content
)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(content)
