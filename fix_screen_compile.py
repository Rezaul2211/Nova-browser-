import re

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    c = f.read()

# Fix ButtonDefaults ambiguous import
c = re.sub(r'^import androidx\.compose\.material3\.ButtonDefaults\s*\n', '', c, flags=re.MULTILINE)
c = c.replace('import androidx.compose.material3.Button', 'import androidx.compose.material3.Button\nimport androidx.compose.material3.ButtonDefaults')

# e: file:///app/src/main/java/com/example/ui/BrowserScreen.kt:133:34 Argument type mismatch: actual type is 'Function1<String, Unit>', but 'Function2<String, Boolean, Unit>' was expected.
c = c.replace('onNavigate = { url -> viewModel.navigateTo(url) }', 'onNavigate = { url, isAi -> viewModel.navigateTo(url) }')

# e: file:///app/src/main/java/com/example/ui/BrowserScreen.kt:537:50 Unresolved reference 'setAiProvider'.
# e: file:///app/src/main/java/com/example/ui/BrowserScreen.kt:538:47 Unresolved reference 'setAiModel'.
c = c.replace('viewModel.setAiProvider', 'viewModel.updateAiProvider')
c = c.replace('viewModel.setAiModel', 'viewModel.updateAiModel')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(c)

