import unittest

from verify_candidate import check_upgrade, identity


class UpgradePreflightTest(unittest.TestCase):
    def setUp(self):
        self.previous = {"package": "dev.davidv.translator.devicetest", "version_code": 342, "signer_sha256": ["a" * 64]}
        self.candidate = dict(self.previous, version_code=352)

    def test_same_signer_increasing_version(self):
        check_upgrade(self.previous, self.candidate)

    def test_reject_other_package(self):
        with self.assertRaisesRegex(ValueError, "Package differs"):
            check_upgrade(self.previous, dict(self.candidate, package="dev.davidv.translator"))

    def test_reject_other_signer(self):
        with self.assertRaisesRegex(ValueError, "Signing certificate differs"):
            check_upgrade(self.previous, dict(self.candidate, signer_sha256=["b" * 64]))

    def test_reject_equal_or_older_versions(self):
        for code in (332, 342):
            with self.subTest(code=code), self.assertRaisesRegex(ValueError, "must increase"):
                check_upgrade(self.previous, dict(self.candidate, version_code=code))

    def test_parse_all_signers_and_numeric_version(self):
        result = identity(
            "package: name='dev.davidv.translator.devicetest' versionCode='352' versionName='0.8.6-test5' platformBuildVersionName='14'\n",
            "Signer #1 certificate SHA-256 digest: " + "B" * 64 + "\nSigner #2 certificate SHA-256 digest: " + "a" * 64 + "\n",
        )
        self.assertEqual(352, result["version_code"])
        self.assertEqual(["a" * 64, "b" * 64], result["signer_sha256"])

    def test_reject_missing_identity(self):
        with self.assertRaises(ValueError):
            identity("", "")


if __name__ == "__main__":
    unittest.main()
