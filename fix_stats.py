import re

with open('app/src/main/java/com/example/privacy/PrivacyStats.kt', 'r') as f:
    content = f.read()

content = content.replace('val adsBlocked: Int = 0,', 'val adsBlocked: Int = 0,\n    val aiAdsBlocked: Int = 0,\n    val aiAdDetections: List<String> = emptyList(),')
content = content.replace('AD_NETWORK("Ad Network"),', 'AD_NETWORK("Ad Network"),\n    AI_DETECTED_AD("AI Detected Ad"),')
content = content.replace('val totalAdsBlocked: Long = 0,', 'val totalAdsBlocked: Long = 0,\n    val totalAiAdsBlocked: Long = 0,')

with open('app/src/main/java/com/example/privacy/PrivacyStats.kt', 'w') as f:
    f.write(content)

