with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    c = f.read()
c = c.replace('SelectedTextAction.COPY -> return', 'SelectedTextAction.COPY -> ""')
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(c)
