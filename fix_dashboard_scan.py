import re

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'r') as f:
    content = f.read()

# Add onAiScan parameter to PrivacyDashboardSheet
content = content.replace('onToggleSiteShield: () -> Unit,', 'onToggleSiteShield: () -> Unit,\n    onAiScan: () -> Unit,')
content = content.replace('onToggleSiteShield = { viewModel.toggleSiteShield() },', 'onToggleSiteShield = { viewModel.toggleSiteShield() },\n                onAiScan = { viewModel.runAiAdDetection(); viewModel.closeSheet() },')

button_ui = """
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAiScan,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Page with AI Ad Detector")
            }
"""

content = content.replace('// Privacy Settings Header', button_ui + '\n\n            // Privacy Settings Header')

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'r') as f:
    screen_content = f.read()

screen_content = screen_content.replace('onToggleSiteShield = { viewModel.toggleSiteShield() },\n                onDismiss = { viewModel.closeSheet() }', 'onToggleSiteShield = { viewModel.toggleSiteShield() },\n                onAiScan = { viewModel.runAiAdDetection(); viewModel.closeSheet() },\n                onDismiss = { viewModel.closeSheet() }')

with open('app/src/main/java/com/example/ui/BrowserScreen.kt', 'w') as f:
    f.write(screen_content)

