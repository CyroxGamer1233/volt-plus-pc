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

    def test_playback_uses_original_sources_display_sized_frames_and_stops_for_game(self):
        source = (ROOT / "crates/horizon-client/src/android/wallpapers.rs").read_text()
        self.assertEqual(source.count('width: 3840, height: 2160'), 3)
        self.assertEqual(source.count('width: 1920, height: 1080'), 2)
        player = (ROOT / "packaging/android/java/dev/horizon/android/WallpaperPlayer.java").read_text()
        self.assertIn('m.setLooping(true)', player)
        self.assertIn('surfaceTexture.setDefaultBufferSize(sourceWidth, sourceHeight)', player)
        self.assertIn('FRAME_INTERVAL_NANOS = 60_000_000L', player)
        self.assertIn('frame_size([item.width, item.height], viewport)',
                      (ROOT / "crates/horizon-client/src/android/launcher.rs").read_text())
        self.assertIn('GLES20.glReadPixels(0, 0, width, height', player)
        launcher = (ROOT / "crates/horizon-client/src/android/launcher.rs").read_text()
        self.assertIn('crate::native_ime::wallpaper_stop();', launcher)
        renderer = (ROOT / "crates/horizon-client-render/src/hud.rs").read_text()
        self.assertIn('Blend::Video => &self.pipeline', renderer)
        self.assertIn('label: Some("Launcher wallpaper video")', renderer)

    def test_no_unverified_or_partial_file_is_installed(self):
        source = (ROOT / "crates/horizon-client/src/android/wallpapers.rs").read_text()
        self.assertIn('take(item.size + 1)', source)
        self.assertLess(source.index('digest_file(&tmp, item.size)'), source.index('std::fs::rename(&tmp, &target)'))
        self.assertIn('std::fs::remove_file(&tmp)', source)


if __name__ == "__main__":
    unittest.main()
