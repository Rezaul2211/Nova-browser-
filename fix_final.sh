#!/bin/bash

# AiAssistantSheet.kt fix
sed -i 's/onClick = onTranslateBangla/onClick = onTranslatePage/g' app/src/main/java/com/example/ui/components/AiAssistantSheet.kt

# BrowserScreen.kt fixes
sed -i 's/viewModel.setAiProvider/viewModel.updateAiProvider/g' app/src/main/java/com/example/ui/BrowserScreen.kt
sed -i 's/viewModel.setAiModel/viewModel.updateAiModel/g' app/src/main/java/com/example/ui/BrowserScreen.kt
sed -i 's/viewModel.setAiDefaultLanguage/viewModel.updateAiDefaultLanguage/g' app/src/main/java/com/example/ui/BrowserScreen.kt
sed -i 's/viewModel.translatePage()/viewModel.translateCurrentPage()/g' app/src/main/java/com/example/ui/BrowserScreen.kt

# BrowserViewModel.kt switch fix
cat << 'VM_EOF' > app/src/main/java/com/example/ui/BrowserViewModel.kt.patch
--- app/src/main/java/com/example/ui/BrowserViewModel.kt
+++ app/src/main/java/com/example/ui/BrowserViewModel.kt
VM_EOF

