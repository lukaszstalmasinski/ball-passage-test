"""Writes a Markdown summary of JaCoCo XML reports, for the GitHub Actions job summary.

Usage: coverage_summary.py TITLE REPORT.xml [REPORT.xml ...]
Missing reports are listed as missing instead of failing, so that a broken test run still shows
its own error rather than one from this script.
"""
import os
import sys
import xml.etree.ElementTree as ET


def counters(element):
    return {c.get('type'): (int(c.get('covered')), int(c.get('missed'))) for c in element.findall('counter')}


def percent(pair):
    covered, missed = pair
    return 100.0 * covered / (covered + missed) if covered + missed else 100.0


def cell(pair):
    covered, missed = pair
    return f'{percent(pair):.1f}% ({covered}/{covered + missed})'


def main(title, paths):
    lines = [f'## Coverage: {title}', '']
    rows, files = [], []
    for path in paths:
        if not os.path.exists(path):
            lines.append(f'- `{path}`: report missing')
            continue
        root = ET.parse(path).getroot()
        total = counters(root)
        # The module directory (core, app) reads better than the report name ("debug" for the app).
        rows.append((path.replace(os.sep, '/').split('/')[0], total))
        for package in root.findall('package'):
            for source in package.findall('sourcefile'):
                c = counters(source)
                if 'LINE' in c:
                    files.append((percent(c['LINE']), f"{package.get('name')}/{source.get('name')}", c))

    if rows:
        lines += ['', '| Module | Lines | Branches |', '|---|---|---|']
        for name, total in rows:
            lines.append(f"| {name} | {cell(total['LINE'])} | {cell(total.get('BRANCH', (0, 0)))} |")
        lines += ['', '<details><summary>Per file, lowest line coverage first</summary>', '',
                  '| File | Lines | Branches |', '|---|---|---|']
        for _, name, c in sorted(files):
            lines.append(f"| `{name}` | {cell(c['LINE'])} | {cell(c.get('BRANCH', (0, 0)))} |")
        lines += ['', '</details>']
    print('\n'.join(lines))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2:])
