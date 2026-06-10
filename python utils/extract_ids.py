import csv
import re
import sys

svg_path = sys.argv[1] if len(sys.argv) > 1 else 'world_one.svg'
csv_path = sys.argv[2] if len(sys.argv) > 2 else 'provincias.csv'

with open(svg_path, 'r', encoding='utf-8') as f:
    content = f.read()

type_map = {
    'SEA': 'SEA',
    'COAST': 'COASTAL',
    'INLAND': 'INLAND',
}

categories = []
for cat in ['SEA', 'COAST', 'INLAND', 'IMPASSABLE']:
    idx = content.find(f'inkscape:label="{cat}"')
    if idx >= 0:
        categories.append((idx, cat))
categories.sort()

provinces = []
for i, (start_pos, cat) in enumerate(categories):
    end_pos = categories[i + 1][0] if i + 1 < len(categories) else len(content)
    section = content[start_pos:end_pos]
    codes = re.findall(r'data-code="([A-Z0-9]{2,5})"', section)
    ptype = type_map.get(cat, cat)
    for code in codes:
        provinces.append((code, ptype))

provinces.sort(key=lambda x: x[0])

with open(csv_path, 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['Province', 'Type'])
    for code, ptype in provinces:
        writer.writerow([code, ptype])

print(f"{len(provinces)} provincias escritas en {csv_path}")
