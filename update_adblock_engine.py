import re

with open('app/src/main/java/com/example/privacy/AdBlockEngine.kt', 'r') as f:
    content = f.read()

# Update totalAiAdsBlocked increment
increment_logic = """
            val newTotalAds = if (evaluation.reason == BlockReason.AD_NETWORK) currentCumul.totalAdsBlocked + 1 else currentCumul.totalAdsBlocked
            val newTotalAiAds = if (evaluation.reason == BlockReason.AI_DETECTED_AD) currentCumul.totalAiAdsBlocked + 1 else currentCumul.totalAiAdsBlocked
"""

content = re.sub(
    r'val newTotalAds = if \(evaluation\.reason == BlockReason\.AD_NETWORK\) currentCumul\.totalAdsBlocked \+ 1 else currentCumul\.totalAdsBlocked',
    increment_logic.strip(),
    content
)

content = content.replace('totalAdsBlocked = newTotalAds,', 'totalAdsBlocked = newTotalAds,\n                totalAiAdsBlocked = newTotalAiAds,')

# Update PagePrivacyStats
page_stats_update = """
            val newAdsBlocked = current.adsBlocked + if (evaluation.reason == BlockReason.AD_NETWORK) 1 else 0
            val newAiAdsBlocked = current.aiAdsBlocked + if (evaluation.reason == BlockReason.AI_DETECTED_AD) 1 else 0
"""
content = re.sub(
    r'val newAdsBlocked = current\.adsBlocked \+ if \(evaluation\.reason == BlockReason\.AD_NETWORK\) 1 else 0',
    page_stats_update.strip(),
    content
)

content = content.replace('adsBlocked = newAdsBlocked,', 'adsBlocked = newAdsBlocked,\n            aiAdsBlocked = newAiAdsBlocked,')

# Add AI detection method
ai_detection_method = """
    fun recordAiAdDetection(tabId: String, pageUrl: String, elementInfo: String): PagePrivacyStats {
        val current = pageStatsMap[tabId] ?: PagePrivacyStats(pageUrl = pageUrl)
        val detections = current.aiAdDetections.toMutableList()
        detections.add(elementInfo)
        
        val updated = current.copy(
            aiAdsBlocked = current.aiAdsBlocked + 1,
            aiAdDetections = detections
        )
        pageStatsMap[tabId] = updated
        
        _cumulativeStats.value = _cumulativeStats.value.copy(
            totalAiAdsBlocked = _cumulativeStats.value.totalAiAdsBlocked + 1
        )
        return updated
    }
"""

content = content.replace('fun getPageStats', ai_detection_method + '\n    fun getPageStats')

with open('app/src/main/java/com/example/privacy/AdBlockEngine.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/privacy/FilterEngine.kt', 'r') as f:
    fe_content = f.read()

fe_content = fe_content.replace('fun getPageStats', 'fun recordAiAdDetection(tabId: String, pageUrl: String, elementInfo: String): PagePrivacyStats {\n        return adBlockEngine.recordAiAdDetection(tabId, pageUrl, elementInfo)\n    }\n\n    fun getPageStats')

with open('app/src/main/java/com/example/privacy/FilterEngine.kt', 'w') as f:
    f.write(fe_content)

