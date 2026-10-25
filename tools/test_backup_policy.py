"""Guard backup privacy and preserve the separate device-migration policy."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

MAIN = Path(__file__).resolve().parent.parent / "app/src/main"
DOMAINS = {"root", "file", "database", "sharedpref", "external", "device_root", "device_file", "device_database", "device_sharedpref"}


class BackupPolicyTest(unittest.TestCase):
    def assert_excludes_all_data(self, section):
        self.assertIsNotNone(section)
        self.assertFalse(section.findall("include"))
        excluded = {e.attrib["domain"] for e in section.findall("exclude") if e.attrib.get("path") == "."}
        self.assertEqual(DOMAINS, excluded)

    def test_legacy_transport_cannot_copy_app_data(self):
        self.assert_excludes_all_data(ET.parse(MAIN / "res/xml/backup_rules.xml").getroot())

    def test_cloud_transport_cannot_copy_app_data(self):
        self.assert_excludes_all_data(ET.parse(MAIN / "res/xml/data_extraction_rules.xml").find("cloud-backup"))

    def test_modern_device_migration_remains_enabled(self):
        transfer = ET.parse(MAIN / "res/xml/data_extraction_rules.xml").find("device-transfer")
        self.assertIsNotNone(transfer)
        self.assertEqual([], list(transfer))

    def test_manifest_uses_both_policies_without_disabling_migration(self):
        app = ET.parse(MAIN / "AndroidManifest.xml").find("application")
        android = "{http://schemas.android.com/apk/res/android}"
        self.assertEqual("true", app.attrib[android + "allowBackup"])
        self.assertEqual("@xml/backup_rules", app.attrib[android + "fullBackupContent"])
        self.assertEqual("@xml/data_extraction_rules", app.attrib[android + "dataExtractionRules"])


if __name__ == "__main__":
    unittest.main()
