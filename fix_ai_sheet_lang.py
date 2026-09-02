import re

with open('app/src/main/java/com/example/ui/components/AiAssistantSheet.kt', 'r') as f:
    content = f.read()

content = content.replace('onTranslateBangla: () -> Unit,', 'onTranslatePage: () -> Unit,')
content = content.replace('onTranslateBangla()', 'onTranslatePage()')
content = content.replace('label = "Translate page to বাংলা",', 'label = "Translate Page",')

with open('app/src/main/java/com/example/ui/components/AiAssistantSheet.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    screen = f.read()

screen = screen.replace('onTranslateBangla = { viewModel.translatePageBangla() },', 'onTranslatePage = { viewModel.translatePage() },')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(screen)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm = f.read()

vm = vm.replace('fun translatePageBangla() {', 'fun translatePage() {')
vm = vm.replace('Translate this webpage into Bengali (বাংলা).', 'Translate this webpage into ${settings.value.preferredLanguage}.')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm)

