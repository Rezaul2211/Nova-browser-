import re

with open('app/src/main/java/com/example/ai/AiModels.kt', 'r') as f:
    c = f.read()

c = c.replace('REWRITE("Rewrite / Simplify"),', '''
    REWRITE_SIMPLIFY("Simplify"),
    REWRITE_SHORTEN("Shorten"),
    REWRITE_PROFESSIONAL("Professional Tone"),
    REWRITE_CASUAL("Casual Tone"),
    REWRITE_ACADEMIC("Academic Tone"),
''')

with open('app/src/main/java/com/example/ai/AiModels.kt', 'w') as f:
    f.write(c)

