"""Writes a Markdown summary of JaCoCo XML reports, for the GitHub Actions job summary.

Usage: coverage_summary.py TITLE REPORT.xml [REPORT.xml ...] [--min MODULE=PERCENT ...]

MODULE is the first directory of a report path (core, app). With --min, the script exits with an
error when a module's line coverage is below PERCENT, after writing the summary. Missing reports are
listed as missing instead of failing, so that a broken test run still shows its own error.
"""
import argparse
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('title')
    parser.add_argument('reports', nargs='+')
    parser.add_argument('--min', action='append', default=[], metavar='MODULE=PERCENT')
    args = parser.parse_args()
    minimum = {k: float(v) for k, v in (m.split('=') for m in args.min)}

    lines = [f'## Coverage: {args.title}', '']
    rows, files, failures = [], [], []
    for path in args.reports:
        if not os.path.exists(path):
            lines.append(f'- `{path}`: report missing')
            continue
        root = ET.parse(path).getroot()
        total = counters(root)
        # The module directory (core, app) reads better than the report name ("debug" for the app).
        module = path.replace(os.sep, '/').split('/')[0]
        rows.append((module, total))
        if module in minimum and percent(total['LINE']) < minimum[module]:
            failures.append(f'{module}: {percent(total["LINE"]):.1f}% of lines covered, minimum is {minimum[module]:g}%')
        for package in root.findall('package'):
            for source in package.findall('sourcefile'):
                c = counters(source)
                if 'LINE' in c:
                    files.append((percent(c['LINE']), f"{package.get('name')}/{source.get('name')}", c))

    if rows:
        if lines[-1]:
            lines.append('')
        lines += ['| Module | Lines | Branches | Minimum (lines) |', '|---|---|---|---|']
        for name, total in rows:
            limit = f'{minimum[name]:g}%' if name in minimum else '–'
            lines.append(f"| {name} | {cell(total['LINE'])} | {cell(total.get('BRANCH', (0, 0)))} | {limit} |")
        lines += ['', '<details><summary>Per file, lowest line coverage first</summary>', '',
                  '| File | Lines | Branches |', '|---|---|---|']
        for _, name, c in sorted(files):
            lines.append(f"| `{name}` | {cell(c['LINE'])} | {cell(c.get('BRANCH', (0, 0)))} |")
        lines += ['', '</details>']
    if failures:
        lines += ['', '**Coverage below the minimum:**', ''] + [f'- {f}' for f in failures]
    print('\n'.join(lines))
    if failures:
        print('\n'.join(failures), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
