"""Run bounded CFF parser regression checks against two MuPDF source trees."""
import argparse
import json
import os
from pathlib import Path
import resource
import subprocess

p = argparse.ArgumentParser()
p.add_argument('baseline', type=Path)
p.add_argument('patched', type=Path)
p.add_argument('--output', type=Path, default=Path('mupdf-probe-results.json'))
args = p.parse_args()
here = Path(__file__).resolve().parent
results = []
env = dict(os.environ, ASAN_OPTIONS='detect_leaks=0:halt_on_error=1', UBSAN_OPTIONS='halt_on_error=1')
# LeakSanitizer needs /proc task access unavailable in this container. Address
# and undefined-behavior checks remain enabled; no leak-testing claim is made.
for label, source in [('baseline', args.baseline.resolve()), ('patched', args.patched.resolve())]:
    exe = here / ('cff-' + label)
    subprocess.run(['gcc', '-O1', '-g', '-no-pie', '-fsanitize=address,undefined',
        '-ffunction-sections', '-fdata-sections', '-I', str(source/'include'),
        '-DMUPDF_CFF_SOURCE="'+str(source/'source/fitz/subset-cff.c')+'"',
        str(here/'cff_bounds_probe.c'), '-Wl,--gc-sections', '-lm', '-o', str(exe)], check=True)
    for case in ['valid', 'extent', 'offsets']:
        def bounded():
            resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
        r = subprocess.run([str(exe), case], capture_output=True, text=True,
            timeout=10, preexec_fn=bounded, env=env)
        results.append({'source': label, 'case': case, 'exit_code': r.returncode,
            'stdout': r.stdout, 'stderr': r.stderr})
        print(label, case, r.returncode, r.stdout.strip())
for filename in ['source/fitz/draw-unpack.c', 'source/pdf/pdf-image.c']:
    r = subprocess.run(['gcc', '-fsyntax-only', '-I', str(args.patched/'include'),
        str(args.patched/filename)], capture_output=True, text=True, timeout=30)
    results.append({'source': 'patched', 'file': filename, 'check': 'syntax',
        'exit_code': r.returncode, 'stderr': r.stderr})
args.output.write_text(json.dumps(results, indent=2)+'\n')
assert all(r['exit_code'] == 0 for r in results if r['source'] == 'patched')
assert next(r for r in results if r.get('case') == 'valid')['exit_code'] == 0
assert next(r for r in results if r['source'] == 'baseline' and r.get('case') == 'extent')['exit_code'] == 3
assert 'heap-buffer-overflow' in next(r for r in results if r['source'] == 'baseline' and r.get('case') == 'offsets')['stderr']
