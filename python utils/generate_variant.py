import csv
import json
import re
import sys
from collections import OrderedDict


def extract_provinces_from_svg(svg_path):
    with open(svg_path, 'r', encoding='utf-8') as f:
        content = f.read()
    codes = set()
    for m in re.finditer(r'data-code="([A-Z0-9]{2,5})"', content):
        codes.add(m.group(1))
    for m in re.finditer(r'\bid="([A-Z0-9]{2,5})"', content):
        codes.add(m.group(1))
    return codes


def parse_adjacency(value):
    value = value.strip()
    if not value:
        return None, None
    parts = value.rsplit(':', 1)
    if len(parts) == 2 and parts[1].isalpha():
        return parts[0].strip(), parts[1].strip().lower()
    return value, None


def read_adjacencies_csv(csv_path):
    adj = {}
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or not row[0].strip():
                continue
            province = row[0].strip().upper()
            neighbors = []
            for cell in row[1:]:
                cell = cell.strip()
                if not cell:
                    continue
                neighbor, coast = parse_adjacency(cell)
                if neighbor:
                    neighbor = neighbor.upper()
                    neighbors.append((neighbor, coast))
            adj[province] = neighbors
    return adj


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <svg_file> <csv_file> [output.json]")
        print(f"  If output.json is omitted, prints to stdout.")
        sys.exit(1)

    svg_path = sys.argv[1]
    csv_path = sys.argv[2]
    output_path = sys.argv[3] if len(sys.argv) > 3 else None

    svg_codes = extract_provinces_from_svg(svg_path)
    csv_adjs = read_adjacencies_csv(csv_path)

    if not svg_codes:
        print("ERROR: no province codes found in SVG")
        sys.exit(1)

    all_provinces = sorted(svg_codes)
    provinces_json = []

    for code in all_provinces:
        adj_map = OrderedDict()
        if code in csv_adjs:
            for neighbor, coast in csv_adjs[code]:
                adj_map[neighbor] = coast

        provinces_json.append(OrderedDict([
            ("name", code),
            ("type", "COASTAL"),
            ("homeNation", None),
            ("supplyCenter", False),
            ("coasts", None),
            ("adjacencies", adj_map)
        ]))

    variant = OrderedDict([
        ("id", "custom-variant"),
        ("name", "Custom Variant"),
        ("colonialRule", False),
        ("provinces", provinces_json),
        ("initialUnits", [])
    ])

    output = json.dumps(variant, indent=2, ensure_ascii=False)

    if output_path:
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(output)
            f.write('\n')
        print(f"Variant written to {output_path} ({len(all_provinces)} provinces)")
    else:
        print(output)


if __name__ == '__main__':
    main()
