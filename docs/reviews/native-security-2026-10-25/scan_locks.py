import argparse
import datetime
import hashlib
import json
from pathlib import Path
import tomllib
import urllib.request

parser = argparse.ArgumentParser(description='Query exact Cargo lock versions and Git commits against OSV.')
parser.add_argument('locks', nargs='+', type=Path)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
locks = args.locks
packages = {}
inputs = []
for path in locks:
    data = path.read_bytes()
    label = str(path)
    inputs.append({'path': label, 'sha256': hashlib.sha256(data).hexdigest()})
    for p in tomllib.loads(data.decode())['package']:
        source = p.get('source', '')
        if source.startswith('registry+'):
            key = ('crates.io', p['name'], p['version'])
            query = {'package': {'ecosystem': key[0], 'name': key[1]}, 'version': key[2]}
        elif source.startswith('git+') and '#' in source:
            key = ('git', source.split('#')[-1])
            query = {'commit': key[1]}
        else:
            continue
        row = packages.setdefault(key, {'query': query, 'packages': [], 'inputs': []})
        identity = {'name': p['name'], 'version': p['version'], 'source': source}
        if identity not in row['packages']:
            row['packages'].append(identity)
        if label not in row['inputs']:
            row['inputs'].append(label)

rows = list(packages.values())
for offset in range(0, len(rows), 100):
    batch = rows[offset:offset+100]
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
        data=json.dumps({'queries': [r['query'] for r in batch]}).encode(),
        headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=90) as response:
        results = json.load(response)['results']
    assert len(results) == len(batch)
    for row, result in zip(batch, results):
        row['advisories'] = result.get('vulns', [])
        row['next_page_token'] = result.get('next_page_token')
        if row['next_page_token']:
            raise RuntimeError('Paginated response requires follow-up; do not claim complete scan')
    print(f'Queried {min(offset+100, len(rows))}/{len(rows)}', flush=True)

report = {'checked_at_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'inputs': inputs, 'query_count': len(rows), 'results': rows,
    'limitations': ['Lockfile inventory, not a selected feature or packaged APK inventory.',
        'No fresh Maven resolution or Gradle distribution scan in this run.',
        'Git no-match is not evidence that nested C/C++ submodules are safe.',
        'Unknown vulnerabilities and actual runtime reachability are outside OSV matching.']}
output = args.output
output.write_text(json.dumps(report, indent=2)+'\n')
for row in rows:
    if row['advisories']:
        print(json.dumps({'packages': row['packages'], 'advisories': row['advisories']}))
print(f'Saved {output}')
