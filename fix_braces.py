with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'r') as f:
    c = f.read()

# count `{` and `}`
open_count = c.count('{')
close_count = c.count('}')

diff = open_count - close_count
print(f"open: {open_count}, close: {close_count}, diff: {diff}")

if diff > 0:
    c += '\n' + ('}' * diff) + '\n'
elif diff < 0:
    # remove trailing braces
    c = c.rstrip()
    for _ in range(-diff):
        if c.endswith('}'):
            c = c[:-1].rstrip()

with open('app/src/main/java/com/example/ui/BrowserViewModel.kt', 'w') as f:
    f.write(c)

