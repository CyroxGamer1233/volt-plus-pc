"""Regression checks for the reported launcher and control-editor issues."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class WallpaperFixTests(unittest.TestCase):
    def read(self, path):
        return (ROOT / path).read_text(encoding="utf-8")

    def test_character_uses_right_side_and_high_resolution_cached_preview(self):
        page = self.read("crates/horizon-client/src/android/launcher_pages.rs")
        wardrobe = self.read("crates/horizon-client/src/android/wardrobe.rs")
        ui = self.read("crates/horizon-client/src/ui_theme.rs")
        self.assertIn("w * 0.76 - 112.0", page)
        self.assertIn("[384, 480]", wardrobe)
        self.assertIn("ui.scene_avatar(", wardrobe)
        self.assertIn("self.avatar_revision != Some(revision)", ui)
        self.assertIn("900 + w > region.w", ui)

    def test_editor_labels_have_contrast_and_correct_actions(self):
        editor = self.read("crates/horizon-client/src/touch_ui.rs").split("pub fn draw_hud_editor(", 1)[1]
        page = self.read("crates/horizon-client/src/android/launcher_pages.rs")
        self.assertIn("let accent_fg = theme.on_accent;", editor)
        self.assertIn('"Save and Exit"', editor)
        self.assertIn('"Java like controlls"', page)
        self.assertIn('let rep_btn_r', editor)
        self.assertIn('"Save"', editor)

    def test_selected_background_shared_with_editor_and_focus_loss(self):
        page = self.read("crates/horizon-client/src/android/launcher_pages.rs")
        self.assertEqual(page.count("self.draw_scene_background(&mut ui, w,"), 2)
        helper = page.split("fn draw_scene_background(", 1)[1].split("fn draw_pages_with(", 1)[0]
        self.assertIn("if let Some(index) = self.catalog.config.wallpaper_index", helper)
        self.assertIn("if self.wallpaper_has_frame", helper)
        self.assertIn("if self.focused && expected > 0", helper)
        self.assertIn("recycle_wallpaper_buffer(&mut b)", page)

if __name__ == "__main__":
    unittest.main()
