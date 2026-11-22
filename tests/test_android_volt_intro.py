"""Checks the five-second startup and its falling logo/letter animation."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class VoltIntroTest(unittest.TestCase):
    def test_five_second_intro_uses_first_frame_and_does_not_wait_for_network(self):
        page = (ROOT / "crates/horizon-client/src/android/launcher_pages.rs").read_text()
        ui = (ROOT / "crates/horizon-client/src/ui_theme.rs").read_text()
        self.assertIn("pub const VOLT_INTRO_SECONDS: f32 = 5.0", ui)
        self.assertIn("intro_start: None", page)
        block = page.split("if !self.anim.intro_done {", 1)[1].split("ui.finish();", 1)[0]
        self.assertIn("self.anim.intro_elapsed >= horizon_client::ui_theme::VOLT_INTRO_SECONDS", block)
        self.assertNotIn("if loading_progress >= 0.999 &&", block)
        self.assertIn("ui.draw_volt_intro(", block)
        self.assertIn("Action::SkipIntro", block)
        self.assertIn("ui.h = full_h", block)

    def test_logo_and_individual_letters_fall_and_fade(self):
        ui = (ROOT / "crates/horizon-client/src/ui_theme.rs").read_text()
        intro = ui.split("pub fn draw_volt_intro(", 1)[1].split("pub fn draw_horizon_intro(", 1)[0]
        self.assertIn('"VOLT PLUS"', intro)
        self.assertIn("volt_intro_fall(elapsed, 0.3)", intro)
        self.assertIn("logo_fall * 105.0", intro)
        letters = ui.split("fn volt_intro_letters(", 1)[1].split("pub fn draw_volt_intro(", 1)[0]
        self.assertIn("text.chars().enumerate()", letters)
        self.assertIn("y + fall * 90.0", letters)
        self.assertIn("fade * entrance", letters)
        self.assertIn("drawable-nodpi/launcher_icon.png", ui)

if __name__ == "__main__":
    unittest.main()
