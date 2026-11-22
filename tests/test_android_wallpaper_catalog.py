"""Original wallpaper downloads must match the supplied files byte-for-byte."""
import hashlib
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class WallpaperCatalogTest(unittest.TestCase):
    def test_manifest_matches_original_mp4s(self):
        source = (ROOT / "crates/horizon-client/src/android/wallpapers.rs").read_text()
        entries = re.findall(r'filename: "([^"]+)", size: ([\d_]+), sha256: "([0-9a-f]{64})"', source)
        self.assertEqual(len(entries), 5)
        for name, length, expected in entries:
            with self.subTest(name=name):
                data = (ROOT / "wallpapers" / name).read_bytes()
                self.assertEqual(len(data), int(length.replace("_", "")))
                self.assertEqual(hashlib.sha256(data).hexdigest(), expected)
                self.assertEqual(data[4:8], b"ftyp")

    def test_no_unverified_or_partial_file_is_installed(self):
        source = (ROOT / "crates/horizon-client/src/android/wallpapers.rs").read_text()
        self.assertIn('take(item.size + 1)', source)
        self.assertLess(source.index('digest_file(&tmp, item.size)'), source.index('std::fs::rename(&tmp, &target)'))
        self.assertIn('std::fs::remove_file(&tmp)', source)


if __name__ == "__main__":
    unittest.main()
