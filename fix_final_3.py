import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "fun handleSelectedText" in line:
        skip = True
    if skip and line.strip() == "}":
        # We need to be careful not to skip everything.
        pass

# Actually, it's easier to find the companion object and everything after it, and rewrite it.
