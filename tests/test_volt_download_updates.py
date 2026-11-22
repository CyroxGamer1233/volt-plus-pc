"""Source regressions for launcher download UX and safe defaults."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
def read(path): return (ROOT / path).read_text()

class DownloadUpdatesTests(unittest.TestCase):
    def test_original_greenhouse_is_bundled_and_default_without_network(self):
        data = (ROOT / 'wallpapers/greenhouse.mp4').read_bytes()
        self.assertEqual(hashlib.sha256(data).hexdigest(), '95e479d8e9428630a8245e6371bcf810ba15e65ae4340047a893572a7f6a0d15')
        src = read('crates/horizon-client/src/android/wallpapers.rs')
        self.assertIn('pub const DEFAULT_INDEX: usize = 3;', src)
        self.assertIn('include_bytes!', src)
        self.assertIn('ensure_bundled_default', src)
        self.assertIn('digest_file(&tmp, item.size)', src)
        pages = read('crates/horizon-client/src/android/launcher_pages.rs')
        self.assertIn('greenhouse_poster.png', pages)
        self.assertIn('effective_index(self.catalog.config.wallpaper_index)', pages)
        self.assertIn('self.start_wallpaper();', pages.split('Some(Action::ClearWallpaper)', 1)[1].split('Some(Action::OpenWallpapers)', 1)[0])

    def test_minigame_handoff_only_after_success(self):
        launcher = read('crates/horizon-client/src/android/launcher.rs')
        pages = read('crates/horizon-client/src/android/launcher_pages.rs')
        self.assertIn('Play Cross the Road', pages)
        self.assertIn('Some(Action::OpenRoadGame)', pages)
        self.assertIn('Some(Action::CloseRoadGame)', pages)
        self.assertIn('self.auto_launch_after_install = false;', pages)
        self.assertIn('std::mem::take(&mut self.auto_launch_after_install)', launcher)
        self.assertIn('!self.install_cancelled.load', launcher)
        self.assertIn('self.action(events)', launcher)
        self.assertIn('if let Err(error) = result', launcher)

    def test_download_ui_and_missing_metrics_are_not_fabricated(self):
        pages = read('crates/horizon-client/src/android/launcher_pages.rs')
        block = pages.split('fn draw_my3_progress_view(',1)[1].split('fn draw_cross_road(',1)[0]
        for v in ('GradientDirection::Horizontal', 'timeline_y', '"Resume"', '"Cancel"', 'pause_action', 'cancel_action'):
            self.assertIn(v, block)
        self.assertNotIn('55.3 * f64::from(overall)', pages)
        self.assertIn('Speed unavailable', pages)

    def test_touch_key_picker_covers_the_registered_keys_and_saves_binding(self):
        options = read('crates/horizon-client-render/src/options_ui.rs')
        self.assertIn('keybinds::available_keys()', options)
        self.assertIn('touch_key_picker', options)
        self.assertIn('return self.bind_key(s, (*token).to_owned());', options)
        self.assertIn('"Next >"', options)
        self.assertIn('"< Previous"', options)
        for path in ('crates/horizon-client/src/game.rs', 'crates/horizon-client/src/game/startup_menu.rs'):
            self.assertIn('touch_key_picker', read(path))

if __name__ == '__main__': unittest.main()
