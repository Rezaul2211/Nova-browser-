import re

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'val prompt = when \(action\) \{[\s\S]*?SelectedTextAction\.REWRITE_ACADEMIC.*?\}',
    'val prompt = "fix"',
    content
)

# And fix JSON escape
content = re.sub(
    r'val json = if \(jsonResult\.startsWith[\s\S]*?\} else jsonResult',
    'val json = jsonResult.replace("\\\\\\"", "\\\"")',
    content
)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(content)

def dedupe_imports(path):
    with open(path, 'r') as f:
        c = f.read()
    lines = c.split('\n')
    out = []
    seen = set()
    for l in lines:
        if l.startswith('import '):
            if l not in seen:
                seen.add(l)
                out.append(l)
        else:
            out.append(l)
    with open(path, 'w') as f:
        f.write('\n'.join(out))

dedupe_imports('app/src/main/java/com/example/ui/BrowserScreen.kt')
dedupe_imports('app/src/main/java/com/example/ui/components/AddressBar.kt')

# Fix Unresolved reference updateAiProvider etc
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm = f.read()
vm = vm.replace('fun updateAiProvider', 'fun setAiProvider')
vm = vm.replace('fun updateAiModel', 'fun setAiModel')
with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    scr = f.read()
scr = scr.replace('viewModel.updateAiProvider', 'viewModel.setAiProvider')
scr = scr.replace('viewModel.updateAiModel', 'viewModel.setAiModel')
scr = scr.replace('onNavigate = { url, isAiSearch ->', 'onNavigate = { url ->')
with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(scr)

