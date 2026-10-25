#!/usr/bin/env python3
"""Verify a signed device-test APK and write its build/upgrade preflight record."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile


def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def identity(badging, signature):
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", badging, re.M)
    certs = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})$", signature, re.M)
    if not package or not certs:
        raise ValueError("Cannot identify APK package, version or verified signing certificates")
    return {
        "package": package[1],
        "version_code": int(package[2]),
        "version_name": package[3],
        "signer_sha256": sorted(cert.lower() for cert in certs),
    }


def check_upgrade(previous, candidate):
    # Deliberately conservative: authorized certificate rotation needs separate review.
    if previous["package"] != candidate["package"]:
        raise ValueError("Package differs from the previous APK; this is not an in-place update")
    if previous["signer_sha256"] != candidate["signer_sha256"]:
        raise ValueError("Signing certificate differs; stop and recover the existing signer")
    if candidate["version_code"] <= previous["version_code"]:
        raise ValueError("Candidate version code must increase over the previous APK")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--previous", type=Path, help="Previously distributed APK, for static upgrade checks")
    parser.add_argument("--abi", default="arm64-v8a")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--build-tools", type=Path, default=None)
    args = parser.parse_args()
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if args.build_tools is None and not sdk:
        parser.error("Set ANDROID_SDK_ROOT or --build-tools")
    build_tools = args.build_tools or Path(sdk) / "build-tools" / "35.0.0"
    repo = Path(__file__).resolve().parent.parent
    if run("git", "-C", str(repo), "status", "--porcelain").strip():
        parser.error("Commit the candidate first; build records require a clean source checkout")

    def inspect(apk):
        signature = run(str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk))
        badging = run(str(build_tools / "aapt"), "dump", "badging", str(apk))
        return identity(badging, signature), badging

    candidate, badging = inspect(args.apk)
    if candidate["package"] != "dev.davidv.translator.devicetest":
        raise ValueError("Expected the isolated device-test package")
    if "application-label:'Translator'" not in badging or "application-debuggable" in badging:
        raise ValueError("Expected generic Translator label and non-debuggable APK")
    if f"native-code: '{args.abi}'\n" not in badging:
        raise ValueError("Unexpected native ABI set")
    native_hashes = {}
    with zipfile.ZipFile(args.apk) as apk:
        names = apk.namelist()
        for name in ("libbindings.so", "libmodel_converter.so", "libc++_shared.so"):
            packaged = apk.read(f"lib/{args.abi}/{name}")
            if packaged != (repo / "app/src/main/jniLibs" / args.abi / name).read_bytes():
                raise ValueError(f"Packaged native library differs from build input: {name}")
        for name in names:
            if name.startswith("lib/") and name.endswith(".so"):
                native_hashes[name] = sha256(apk.read(name))
        catalogs = [name for name in names if re.fullmatch(r"assets/index_v\d+\.json", name)]
        if len(catalogs) != 1:
            raise ValueError("Expected one bundled versioned catalog")
        catalog = catalogs[0]
        if apk.read(catalog) != (repo / "app/src/main" / catalog).read_bytes():
            raise ValueError("Packaged catalog differs from source")
        catalog_hash = sha256(apk.read(catalog))
    previous = None
    if args.previous:
        previous, _ = inspect(args.previous)
        check_upgrade(previous, candidate)
        previous["apk_sha256"] = sha256(args.previous.read_bytes())
    record = {
        "schema": 1,
        "source_commit": run("git", "-C", str(repo), "rev-parse", "HEAD").strip(),
        "source_tree": run("git", "-C", str(repo), "rev-parse", "HEAD^{tree}").strip(),
        "apk_sha256": sha256(args.apk.read_bytes()),
        "identity": candidate,
        "native_libraries": native_hashes,
        "catalog": {"path": catalog, "sha256": catalog_hash},
        "previous_apk": previous,
        "static_upgrade_preflight": "passed" if previous else "not_run",
        "device_launch": "unverified",
        "runtime_upgrade_data_retention": "unverified",
    }
    args.output.write_text(json.dumps(record, indent=2) + "\n")
    print(f"Verified {args.apk.name}; static upgrade preflight: {record['static_upgrade_preflight']}")


if __name__ == "__main__":
    main()
