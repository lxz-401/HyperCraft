#!/usr/bin/env python3
"""
Locale string synchronization script for HyperCeiler.

Usage:
    python3 scripts/sync_strings.py [options]

Options:
    --check        Check for missing translations without writing files
    --verbose      Print detailed information
    --base DIR     Base strings.xml directory (default: app/src/main/res/values)
    --locales DIR  Locales directory (default: app/src/main/res)

This script ensures all locale files have the same string keys as the base
English values/strings.xml. Missing keys are added with translatable="false"
so the app falls back to the English translation until proper translation is
provided.
"""

import argparse
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, List, Set, Tuple


def parse_strings_file(filepath: Path) -> Dict[str, Dict[str, str]]:
    """Parse a strings.xml file and return a dict of string name -> attributes/text."""
    tree = ET.parse(filepath)
    root = tree.getroot()
    strings = {}

    for elem in root:
        if elem.tag == "string":
            name = elem.get("name")
            if name:
                strings[name] = {
                    "text": elem.text or "",
                    "translatable": elem.get("translatable", "true"),
                }
    return strings


def ensure_locale_complete(
    base_strings: Dict[str, Dict[str, str]],
    locale_dir: Path,
    check_only: bool = False,
    verbose: bool = False
) -> Tuple[int, int, List[str]]:
    """
    Ensure a locale directory has all base string keys.

    Returns:
        (missing_count, added_count, missing_keys)
    """
    locale_name = locale_dir.name
    strings_file = locale_dir / "strings.xml"

    if not strings_file.exists():
        if check_only:
            if verbose:
                print(f"  [MISSING] {locale_name}/strings.xml does not exist")
            return len(base_strings), 0, sorted(base_strings.keys())
        # Create the locale directory and file
        locale_dir.mkdir(parents=True, exist_ok=True)
        if verbose:
            print(f"  [CREATE] {locale_name}/strings.xml")
        new_root = ET.Element("resources")
        for name, data in base_strings.items():
            s = ET.SubElement(new_root, "string", {"name": name, "translatable": "false"})
            s.text = data["text"]
        ET.ElementTree(new_root).write(strings_file, encoding="utf-8", xml_declaration=True)
        return 0, len(base_strings), []

    # Parse existing locale strings
    locale_strings = parse_strings_file(strings_file)
    missing_keys: List[str] = []
    for base_key in base_strings:
        if base_key not in locale_strings:
            missing_keys.append(base_key)

    if not missing_keys:
        return 0, 0, []

    if check_only:
        return len(missing_keys), 0, missing_keys

    # Add missing keys to the locale file
    tree = ET.parse(strings_file)
    root = tree.getroot()
    added_count = 0
    for key in missing_keys:
        s = ET.SubElement(root, "string", {"name": key, "translatable": "false"})
        s.text = base_strings[key]["text"]
        added_count += 1

    if added_count > 0:
        if verbose:
            print(f"  [ADD {added_count}] {locale_name}/strings.xml")
        ET.ElementTree(root).write(strings_file, encoding="utf-8", xml_declaration=True)

    return len(missing_keys), added_count, missing_keys


def find_locale_dirs(locales_dir: Path) -> List[Path]:
    """Find all values-* locale directories."""
    locale_dirs = []
    for item in locales_dir.iterdir():
        if item.is_dir() and item.name.startswith("values-"):
            locale_dirs.append(item)
    return sorted(locale_dirs)


def main():
    parser = argparse.ArgumentParser(description="Sync locale strings with base strings")
    parser.add_argument("--check", action="store_true", help="Check without writing")
    parser.add_argument("--verbose", action="store_true", help="Verbose output")
    parser.add_argument(
        "--base",
        default="app/src/main/res/values",
        help="Base strings.xml directory"
    )
    parser.add_argument(
        "--locales",
        default="app/src/main/res",
        help="Locales directory"
    )
    args = parser.parse_args()

    base_dir = Path(args.base)
    locales_dir = Path(args.locales)
    base_strings_file = base_dir / "strings.xml"

    if not base_strings_file.exists():
        print(f"ERROR: Base strings file not found: {base_strings_file}", file=sys.stderr)
        sys.exit(1)

    if not locales_dir.exists():
        print(f"ERROR: Locales directory not found: {locales_dir}", file=sys.stderr)
        sys.exit(1)

    # Parse base strings
    base_strings = parse_strings_file(base_strings_file)
    if args.verbose:
        print(f"Base strings: {len(base_strings)} in {base_strings_file}")

    # Find all locale directories
    locale_dirs = find_locale_dirs(locales_dir)
    if args.verbose:
        print(f"Found {len(locale_dirs)} locale directories:")
        for ld in locale_dirs:
            print(f"  - {ld.name}")

    total_missing = 0
    total_added = 0
    locales_with_issues: List[str] = []

    for locale_dir in locale_dirs:
        missing, added, missing_keys = ensure_locale_complete(
            base_strings, locale_dir, args.check, args.verbose
        )
        if missing > 0:
            total_missing += missing
            if not args.check:
                total_added += added
            locales_with_issues.append(f"{locale_dir.name} ({missing} missing)")

    # Summary
    print(f"\n=== Sync Summary ===")
    print(f"Locales checked: {len(locale_dirs)}")

    if locales_with_issues:
        print(f"Locales with missing keys:")
        for issue in locales_with_issues:
            print(f"  - {issue}")
    else:
        print("All locales complete!")

    if args.check:
        print(f"\n[CHECK MODE] {total_missing} missing keys total (no changes written)")
        sys.exit(1 if total_missing > 0 else 0)
    else:
        print(f"\n[WRITE MODE] Added {total_added} keys to {len(locales_with_issues)} locales")
        sys.exit(0)


if __name__ == "__main__":
    main()