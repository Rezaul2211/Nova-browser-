import re

with open('app/src/main/java/com/example/ai/AiModels.kt', 'r') as f:
    content = f.read()

content = content.replace('TRANSLATE_BANGLA("Translate to বাংলা"),', 'TRANSLATE_BANGLA("Translate to বাংলা"),\n    ASK("Ask AUREN"),')

with open('app/src/main/java/com/example/ai/AiModels.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    vm = f.read()

vm = vm.replace('SelectedTextAction.COPY -> return', 'SelectedTextAction.COPY -> return\n            SelectedTextAction.ASK -> "Answer a question about the following text:\\n\\n\\"$selectedText\\""')

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(vm)

