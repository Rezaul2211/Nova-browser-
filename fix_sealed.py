with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    c = f.read()

c = c.replace('data class SelectedTextMenu(val text: String) : ActiveSheet\n\nclass BrowserViewModel', 'data class SelectedTextMenu(val text: String) : ActiveSheet\n}\n\nclass BrowserViewModel')
c = c.replace('data class SelectedTextMenu(val text: String) : ActiveSheet\nclass BrowserViewModel', 'data class SelectedTextMenu(val text: String) : ActiveSheet\n}\n\nclass BrowserViewModel')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(c)

