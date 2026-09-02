import re

with open('app/src/main/java/com/example/privacy/FilterRules.kt', 'r') as f:
    content = f.read()

new_patterns = """        Regex(".*[/?&]ad_url=.*", RegexOption.IGNORE_CASE),
        Regex(".*/ads?\\.(js|php|html|gif|png).*", RegexOption.IGNORE_CASE),
        Regex(".*/adserver/.*", RegexOption.IGNORE_CASE),
        Regex(".*/advert(s|isement|ising)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/popads.*", RegexOption.IGNORE_CASE),
        Regex(".*/banner(s)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/affiliate(s)?/.*", RegexOption.IGNORE_CASE),
        Regex(".*/track(er|ing)?\\.(js|php).*", RegexOption.IGNORE_CASE),
        Regex(".*/pixel\\.(gif|png|js).*", RegexOption.IGNORE_CASE),
        Regex(".*/telemetry(/|\\?).*", RegexOption.IGNORE_CASE),
        Regex(".*/beacon\\.(js|gif).*", RegexOption.IGNORE_CASE),
        Regex(".*/gtag/js.*", RegexOption.IGNORE_CASE),
        Regex(".*/fbevents\\.js.*", RegexOption.IGNORE_CASE),
        Regex(".*/analytics\\.js.*", RegexOption.IGNORE_CASE),
        Regex(".*(/ad/|/ads/|/advert/|/advertisement/|/banner/|/sponsor/|/promoted/|/doubleclick/|/pagead/|/googlesyndication/|/googleadservices/|/adservice/|/prebid/|/bid/|/vast/|/vmap/).*", RegexOption.IGNORE_CASE)
"""

content = re.sub(
    r'val AD_PATH_PATTERNS: List<Regex> = listOf\([\s\S]*?\n    \)',
    'val AD_PATH_PATTERNS: List<Regex> = listOf(\n' + new_patterns + '    )',
    content
)

with open('app/src/main/java/com/example/privacy/FilterRules.kt', 'w') as f:
    f.write(content)
