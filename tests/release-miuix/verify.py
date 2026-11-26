#!/usr/bin/env python3
"""Check actual release DEX definitions for Miuix's runtime-loaded support types."""
from pathlib import Path
import struct
import sys
import zipfile

root = Path(__file__).resolve().parents[2]
apk = Path(sys.argv[1]) if len(sys.argv) > 1 else max(
    (root / 'app/build/outputs/apk/release').glob('*.apk'), key=lambda p: p.stat().st_mtime)
required = {'Lcom/fan/support/' + name + ';' for name in (
    'cardview/BackgroundOutline', 'cardview/CardView', 'drawable/CardDrawable',
    'drawable/CardDrawable$Builder', 'drawable/CardDrawable$CardState',
    'drawable/CardStateDrawable', 'drawable/CardStateDrawable$AlphaBlendingState',
    'drawable/DrawableUtils')}
found = set()
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if not (name.startswith('classes') and name.endswith('.dex')):
            continue
        data = archive.read(name)
        strings_count, strings_offset, types_count, types_offset = struct.unpack_from('<4I', data, 56)
        strings = []
        for index in range(strings_count):
            offset = struct.unpack_from('<I', data, strings_offset + index * 4)[0]
            while data[offset] & 128:
                offset += 1
            offset += 1
            strings.append(data[offset:data.index(0, offset)].decode('utf-8', 'replace'))
        types = struct.unpack_from('<' + str(types_count) + 'I', data, types_offset)
        count, offset = struct.unpack_from('<2I', data, 96)
        for index in range(count):
            type_index = struct.unpack_from('<I', data, offset + index * 32)[0]
            found.add(strings[types[type_index]])
missing = required - found
if missing:
    raise SystemExit('Missing Miuix release classes: ' + ', '.join(sorted(missing)))
print('Miuix release support definitions: 8/8 PASS (' + apk.name + ')')
