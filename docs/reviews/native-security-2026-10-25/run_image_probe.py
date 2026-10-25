"""Link and test baseline/patched MuPDF libraries built by mupdf-sys."""
import argparse
import hashlib
import json
from pathlib import Path
import resource
import subprocess

p = argparse.ArgumentParser()
p.add_argument('baseline_build', type=Path)
p.add_argument('patched_build', type=Path)
p.add_argument('--output', required=True, type=Path)
args = p.parse_args()
here = Path(__file__).resolve().parent
results = []
for label, build in [('baseline', args.baseline_build.resolve()), ('patched', args.patched_build.resolve())]:
    hashes = {f: hashlib.sha256((build/f).read_bytes()).hexdigest() for f in
        ['source/fitz/subset-cff.c', 'source/fitz/draw-unpack.c', 'source/pdf/pdf-image.c']}
    exe = here / ('image-' + label)
    subprocess.run(['gcc', '-I', str(build/'include'), str(here/'image_bounds_probe.c'),
        str(build/'libmupdf.a'), str(build/'libmupdf-third.a'), '-lm', '-lpthread',
        '-o', str(exe)], check=True)
    for case in ['valid', 'height', 'pad', 'skip']:
        def bounded():
            resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
        r = subprocess.run([str(exe), case], capture_output=True, text=True,
            timeout=10, preexec_fn=bounded)
        results.append({'source': label, 'source_sha256': hashes, 'case': case,
            'exit_code': r.returncode, 'stdout': r.stdout, 'stderr': r.stderr})
        print(label, case, r.returncode, r.stdout.strip())
args.output.write_text(json.dumps(results, indent=2)+'\n')
assert results[0]['source_sha256'] != results[4]['source_sha256']
assert all(r['exit_code'] == 0 for r in results if r['source'] == 'patched' or r['case'] == 'valid')
assert all(r['exit_code'] == 3 for r in results if r['source'] == 'baseline' and r['case'] != 'valid')
