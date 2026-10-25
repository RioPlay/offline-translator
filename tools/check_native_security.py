#!/usr/bin/env python3
"""Check reviewed native pins and the MuPDF sources staged by its build script.

This verifies the prepared fixes, not the absence of all vulnerabilities.
See docs/security-status.md for outstanding findings and coverage limits.
"""
import argparse
import hashlib
import json
from pathlib import Path
import tomllib
from urllib.parse import urlsplit, parse_qs

ROOT = Path(__file__).resolve().parents[1]
TARGETS = {"arm64-v8a": "aarch64-linux-android", "armeabi-v7a": "armv7-linux-androideabi"}


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def check_lock(path, pins, required):
    packages = tomllib.loads(path.read_text())["package"]
    seen = set()
    for package in packages:
        name = package["name"]
        component = ("engine" if name == "translator" or name.startswith("translator-")
                     else "mupdf_wrapper" if name in {"mupdf", "mupdf-sys"}
                     else "cld2" if name in {"cld2", "cld2-sys"} else None)
        if component:
            expected = pins[component]
            source = urlsplit(package.get("source", "").removeprefix("git+"))
            if (source.scheme != "https" or source.netloc != "github.com"
                    or source.path.removeprefix("/").removesuffix(".git") != expected["repository"]
                    or source.fragment != expected["revision"]
                    or parse_qs(source.query).get("rev") != [expected["revision"]]):
                raise ValueError(f"{path}: {name} does not use its reviewed immutable source")
            seen.add(component)
        if (name, package["version"]) in {
                ("lopdf", "0.40.0"), ("lopdf", "0.41.0"), ("regex", "0.1.80"),
                ("rustc-serialize", "0.3.25"), ("thread_local", "0.2.7"),
                ("anyhow", "1.0.102"), ("crossbeam-epoch", "0.9.18")}:
            raise ValueError(f"{path}: previously reviewed affected dependency returned: {name}")
    if not required <= seen:
        raise ValueError(f"{path}: missing reviewed components: {sorted(required - seen)}")
    return {"sha256": sha256(path), "components": sorted(seen)}


def check_mupdf_builds(build_root, hashes):
    builds = sorted(build_root.glob("mupdf-sys-*/out/build"))
    if not builds:
        raise ValueError(f"No MuPDF build source found in {build_root}")
    for build in builds:
        for name, expected in hashes.items():
            if not (build / name).is_file() or sha256(build / name) != expected:
                raise ValueError(f"Unreviewed MuPDF source in {build}: {name}")
    return len(builds)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--abi", choices=TARGETS)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    if bool(args.abi) != bool(args.apk):
        parser.error("--abi and --apk must be provided together")
    pins = json.loads((ROOT / "tools/native-security-pins.json").read_text())
    report = {"pins": pins, "locks": {}}
    for component, required in [("bindings", {"engine", "mupdf_wrapper", "cld2"}),
                                ("model-converter", {"engine"})]:
        lock = ROOT / "app/src/main" / component / "Cargo.lock"
        report["locks"][component] = check_lock(lock, pins, required)
    if args.abi:
        build_root = ROOT / "app/src/main/bindings/target" / TARGETS[args.abi] / "release/build"
        report["verified_mupdf_builds"] = check_mupdf_builds(build_root, pins["mupdf"]["source_sha256"])
        report["abi"] = args.abi
        report["apk_sha256"] = sha256(args.apk)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + "\n")
    print("Reviewed native security pins verified" + (f" for {args.abi}" if args.abi else ""))


if __name__ == "__main__":
    main()
