with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    c = f.read()

c = c.replace('compile("\\[.*?\\]"', 'compile("\\\\[.*?\\\\]"')

if not c.endswith('}'):
    c += "\\n}"

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(c)

