import csv
import json
import sys
from collections import OrderedDict, defaultdict


def parse_adjacency(value):
    value = value.strip()
    if not value:
        return None, None
    parts = value.rsplit(':', 1)
    if len(parts) == 2 and parts[1].isalpha():
        return parts[0].strip().upper(), parts[1].strip().lower()
    return value.strip().upper(), None


def csv_to_variant(csv_path, variant_id='world-variant', variant_name='World Variant'):
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)

        provinces = []
        nation_units = defaultdict(list)

        for row in reader:
            if not row or not row[0].strip():
                continue

            code = row[0].strip().upper()
            ptype = row[1].strip().upper() if len(row) > 1 and row[1].strip() else 'COASTAL'
            sc_raw = row[2].strip().upper() if len(row) > 2 else ''
            nation = row[3].strip() if len(row) > 3 and row[3].strip() else None
            unit = row[4].strip().upper() if len(row) > 4 and row[4].strip() else None

            supply_center = sc_raw == 'TRUE'

            adjacencies = OrderedDict()
            for cell in row[5:]:
                neighbor, coast = parse_adjacency(cell)
                if neighbor:
                    adjacencies[neighbor] = coast

            if nation and unit:
                nation_units[nation].append(OrderedDict([
                    ('nation', nation),
                    ('unitType', unit),
                    ('province', code)
                ]))

            provinces.append(OrderedDict([
                ('name', code),
                ('type', ptype),
                ('homeNation', nation if nation else None),
                ('supplyCenter', supply_center),
                ('coasts', None),
                ('adjacencies', adjacencies)
            ]))

    initial_units = []
    for nation in sorted(nation_units.keys()):
        initial_units.append(OrderedDict([
            ('nation', nation),
            ('units', nation_units[nation])
        ]))

    variant = OrderedDict([
        ('id', variant_id),
        ('name', variant_name),
        ('colonialRule', False),
        ('provinces', provinces),
        ('initialUnits', initial_units)
    ])

    return variant


def main():
    if len(sys.argv) < 2:
        print(f'Usage: {sys.argv[0]} <input.csv> [output.json]')
        print(f'  If output.json is omitted, prints to stdout.')
        sys.exit(1)

    csv_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else None

    variant = csv_to_variant(csv_path)

    output = json.dumps(variant, indent=2, ensure_ascii=False)

    if output_path:
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(output)
            f.write('\n')
        total = len(variant['provinces'])
        nations = len(variant['initialUnits'])
        print(f'{total} provincias, {nations} naciones -> {output_path}')
    else:
        print(output)


if __name__ == '__main__':
    main()
