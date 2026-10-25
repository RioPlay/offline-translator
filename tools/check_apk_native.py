#!/usr/bin/env python3
"""Check native ABI and 16 KB ELF/ZIP alignment; this is not a runtime test."""

import argparse
from pathlib import Path
import struct
import zipfile

ABIS = {"arm64-v8a": (2, 183), "armeabi-v7a": (1, 40), "x86_64": (2, 62), "x86": (1, 3)}


def check_elf(data, abi):
    elf_class, machine = ABIS[abi]
    if data[:4] != b"\x7fELF" or data[4:6] != bytes((elf_class, 1)):
        raise ValueError("Invalid ELF class/byte order for ABI")
    if struct.unpack_from("<H", data, 18)[0] != machine:
        raise ValueError("ELF machine does not match APK ABI")
    is64 = elf_class == 2
    offset = struct.unpack_from("<Q" if is64 else "<I", data, 32 if is64 else 28)[0]
    size, count = struct.unpack_from("<HH", data, 54 if is64 else 42)
    if size < (56 if is64 else 32) or not count:
        raise ValueError("Invalid ELF program table")
    loads = 0
    for index in range(count):
        start = offset + index * size
        if struct.unpack_from("<I", data, start)[0] != 1:
            continue
        loads += 1
        if is64:
            file_offset, address = struct.unpack_from("<QQ", data, start + 8)
            alignment = struct.unpack_from("<Q", data, start + 48)[0]
            if alignment < 16384 or alignment & (alignment - 1) or (address - file_offset) % 16384:
                raise ValueError("64-bit LOAD segment is not 16 KB aligned")
    if not loads:
        raise ValueError("ELF has no LOAD segments")


def check_apk(path, abi):
    count = 0
    names = set()
    with zipfile.ZipFile(path) as apk, path.open("rb") as raw:
        for entry in apk.infolist():
            if not entry.filename.startswith("lib/") or not entry.filename.endswith(".so"):
                continue
            if not entry.filename.startswith(f"lib/{abi}/"):
                raise ValueError("Unexpected ABI in split APK")
            check_elf(apk.read(entry), abi)
            if ABIS[abi][0] == 2 and entry.compress_type == zipfile.ZIP_STORED:
                raw.seek(entry.header_offset + 26)
                name_size, extra_size = struct.unpack("<HH", raw.read(4))
                if (entry.header_offset + 30 + name_size + extra_size) % 16384:
                    raise ValueError("Uncompressed native library is not ZIP-aligned to 16 KB")
            names.add(Path(entry.filename).name)
            count += 1
    if not {"libbindings.so", "libmodel_converter.so", "libc++_shared.so"}.issubset(names):
        raise ValueError("Required native library missing")
    print(f"{path.name}: {count} native libraries pass {abi} ABI/alignment checks")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--abi", required=True, choices=ABIS)
    args = parser.parse_args()
    check_apk(args.apk, args.abi)
