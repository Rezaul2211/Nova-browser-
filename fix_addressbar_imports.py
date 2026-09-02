import re

with open('app/src/main/java/com/example/ui/components/AddressBar.kt', 'r') as f:
    content = f.read()

imports = """
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
"""

if 'import androidx.compose.material3.Button' not in content:
    content = content.replace('import androidx.compose.material3.Icon', imports.strip() + '\nimport androidx.compose.material3.Icon')

with open('app/src/main/java/com/example/ui/components/AddressBar.kt', 'w') as f:
    f.write(content)
