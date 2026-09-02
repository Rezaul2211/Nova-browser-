import re

with open('app/src/main/java/com/example/privacy/FilterRules.kt', 'r') as f:
    content = f.read()

content = content.replace(r'".*/ads?\.(js|php|html|gif|png).*"', r'".*/ads?\\.(js|php|html|gif|png).*"')
content = content.replace(r'".*/track(er|ing)?\.(js|php).*"', r'".*/track(er|ing)?\\.(js|php).*"')
content = content.replace(r'".*/pixel\.(gif|png|js).*"', r'".*/pixel\\.(gif|png|js).*"')
content = content.replace(r'".*/telemetry(/|\?).*"', r'".*/telemetry(/|\\?).*"')
content = content.replace(r'".*/beacon\.(js|gif).*"', r'".*/beacon\\.(js|gif).*"')
content = content.replace(r'".*/fbevents\.js.*"', r'".*/fbevents\\.js.*"')
content = content.replace(r'".*/analytics\.js.*"', r'".*/analytics\\.js.*"')

with open('app/src/main/java/com/example/privacy/FilterRules.kt', 'w') as f:
    f.write(content)
