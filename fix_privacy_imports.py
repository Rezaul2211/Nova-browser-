import re

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'r') as f:
    content = f.read()

if 'import androidx.compose.material.icons.filled.AutoAwesome' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Security', 'import androidx.compose.material.icons.filled.Security\nimport androidx.compose.material.icons.filled.AutoAwesome')

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'w') as f:
    f.write(content)
