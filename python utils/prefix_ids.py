import re
import sys

svg_path = sys.argv[1] if len(sys.argv) > 1 else 'world_one.svg'

with open(svg_path, 'r', encoding='utf-8') as f:
    content = f.read()

def prefix_province_id(match):
    tag = match.group(1)
    id_code = match.group(2)
    rest = match.group(3)
    if f'provincia-' in tag + rest:
        return match.group(0)
    if f'data-code="{id_code}"' in tag + rest:
        return tag + f'id="provincia-{id_code}"' + rest
    return match.group(0)

content = re.sub(
    r'(<[a-z]+[^>]*?)\bid="([A-Z0-9]{2,5})"([^>]*?/?>)',
    prefix_province_id,
    content
)

with open(svg_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"Prefixed province IDs in {svg_path}")
