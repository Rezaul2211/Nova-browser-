import re

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'r') as f:
    content = f.read()

# Add AI detection display to Page Stats
page_stats_ai = """
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("AI Detected & Blocked:", style = MaterialTheme.typography.bodySmall)
                            Text("${pageStats.aiAdsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
"""

content = content.replace('Text("${pageStats.adsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))\n                        }', 'Text("${pageStats.adsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))\n                        }\n' + page_stats_ai)

cumul_stats_ai = """
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total AI Ads Detected:", style = MaterialTheme.typography.bodySmall)
                            Text("${cumulativeStats.totalAiAdsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
"""
content = content.replace('Text("${cumulativeStats.totalAdsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))\n                        }', 'Text("${cumulativeStats.totalAdsBlocked}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))\n                        }\n' + cumul_stats_ai)


ai_reports_list = """
            if (pageStats.aiAdDetections.isNotEmpty()) {
                item {
                    Text(
                        text = "AI Ad Detections",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    
                    pageStats.aiAdDetections.forEach { detection ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = detection,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                                )
                            }
                        }
                    }
                }
            }
"""

content = content.replace('if (pageStats.blockedDomains.isNotEmpty()) {', ai_reports_list + '\n            if (pageStats.blockedDomains.isNotEmpty()) {')

with open('app/src/main/java/com/example/ui/components/PrivacyDashboardSheet.kt', 'w') as f:
    f.write(content)

