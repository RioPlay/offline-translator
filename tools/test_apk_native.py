import struct
import unittest

from check_apk_native import check_elf


def elf64(alignment=16384, address=0, machine=183):
    data = bytearray(120)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 18, machine)
    struct.pack_into("<Q", data, 32, 64)
    struct.pack_into("<HH", data, 54, 56, 1)
    struct.pack_into("<I", data, 64, 1)
    struct.pack_into("<Q", data, 80, address)
    struct.pack_into("<Q", data, 112, alignment)
    return data


class NativeCompatibilityTest(unittest.TestCase):
    def test_accepts_16k_and_64k_load_alignment(self):
        for alignment in (16384, 65536):
            check_elf(elf64(alignment), "arm64-v8a")

    def test_rejects_4k_alignment(self):
        with self.assertRaisesRegex(ValueError, "16 KB"):
            check_elf(elf64(4096), "arm64-v8a")

    def test_rejects_incongruent_file_and_virtual_offsets(self):
        with self.assertRaisesRegex(ValueError, "16 KB"):
            check_elf(elf64(address=4096), "arm64-v8a")

    def test_rejects_wrong_machine(self):
        with self.assertRaisesRegex(ValueError, "machine"):
            check_elf(elf64(machine=62), "arm64-v8a")


if __name__ == "__main__":
    unittest.main()
