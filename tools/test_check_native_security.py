import hashlib
import json
from pathlib import Path
import tempfile
import tomllib
import unittest

from check_native_security import ROOT, check_lock, check_mupdf_builds


class NativeSecurityRegressionTests(unittest.TestCase):
    def setUp(self):
        self.pins = json.loads((ROOT / "tools/native-security-pins.json").read_text())
        self.lock = (ROOT / "app/src/main/bindings/Cargo.lock").read_text()

    def check_changed_lock(self, contents):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "Cargo.lock"
            path.write_text(contents)
            check_lock(path, self.pins, {"engine", "mupdf_wrapper", "cld2"})

    def test_old_mupdf_source_is_rejected_even_at_same_crate_version(self):
        packages = tomllib.loads(self.lock)["package"]
        current = next(p["source"] for p in packages if p["name"] == "mupdf")
        old = "git+https://github.com/DavidVentura/mupdf-rs#6f695fae38dcd1d4ed8f39f56b177042f3ff11a5"
        with self.assertRaisesRegex(ValueError, "reviewed immutable source"):
            self.check_changed_lock(self.lock.replace(current, old))

    def test_reintroduced_old_parser_is_rejected(self):
        extra = '\n[[package]]\nname = "lopdf"\nversion = "0.40.0"\n'
        with self.assertRaisesRegex(ValueError, "affected dependency returned"):
            self.check_changed_lock(self.lock + extra)

    def test_missing_build_evidence_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, "No MuPDF build source"):
                check_mupdf_builds(Path(tmp), {"source/test.c": "unused"})

    def test_stale_native_build_source_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "mupdf-sys-test/out/build/source/test.c"
            source.parent.mkdir(parents=True)
            source.write_bytes(b"reviewed source")
            hashes = {"source/test.c": hashlib.sha256(source.read_bytes()).hexdigest()}
            self.assertEqual(check_mupdf_builds(root, hashes), 1)
            source.write_bytes(b"old source from cached build")
            with self.assertRaisesRegex(ValueError, "Unreviewed MuPDF source"):
                check_mupdf_builds(root, hashes)


if __name__ == "__main__":
    unittest.main()
