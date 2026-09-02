import re

def add_spacer(path):
    with open(path, 'r') as f:
        c = f.read()
    if 'import androidx.compose.foundation.layout.width' not in c:
        c = c.replace('import androidx.compose.foundation.layout.fillMaxWidth', 'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.width')
    if 'import androidx.compose.foundation.layout.Spacer' not in c:
        c = c.replace('import androidx.compose.foundation.layout.width', 'import androidx.compose.foundation.layout.width\nimport androidx.compose.foundation.layout.Spacer')
    with open(path, 'w') as f:
        f.write(c)

add_spacer('app/src/main/java/com/example/ui/BrowserScreen.kt')
add_spacer('app/src/main/java/com/example/ui/components/AddressBar.kt')
