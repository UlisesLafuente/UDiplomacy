import re
import sys

svg_path = sys.argv[1] if len(sys.argv) > 1 else print("indica el path del svg en un argumento")

with open(svg_path, 'r', encoding='utf-8') as f:
    content = f.read()

def add_datacode(match):
    full = match.group(0)
    code = match.group(1)
    if f'data-code="{code}"' in full:
        return full
    return full.replace(f'id="{code}"', f'id="{code}" data-code="{code}"')

content = re.sub(r'id="([A-Z0-9]{2,5})"', add_datacode, content)

with open(svg_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Updated {svg_path}")
