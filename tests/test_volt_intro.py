"""Regression checks for the video startup and asynchronous avatar preview."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
def source(path):
    return (ROOT / path).read_text(encoding="utf-8")

class VoltIntroTests(unittest.TestCase):
    def test_original_video_and_both_packaging_paths(self):
        video = ROOT / 'packaging/android/media/intro.mp4'
        self.assertEqual(hashlib.sha256(video.read_bytes()).hexdigest(),
                         '7917a156073ebf9b6534881764eb350e7b51f9929d61416d8d758868c722cccd')
        gradle = source('packaging/android/build.gradle')
        standalone = source('packaging/android/build_apk.py')
        self.assertIn("'media'", gradle)
        self.assertIn("noCompress += ['mp4']", gradle)
        self.assertIn('HERE / "media" / "intro.mp4"', standalone)
        self.assertIn('"-0", "mp4"', standalone)

    def test_overlay_waits_for_native_frame_and_handles_failures(self):
        java = source('packaging/android/java/dev/horizon/android/HorizonActivity.java')
        pages = source('crates/horizon-client/src/android/launcher_pages.rs')
        self.assertIn('prepareAsync()', java)
        self.assertIn('getAssets().openFd("intro.mp4")', java)
        self.assertIn('!nativeReady || !videoFinished', java)
        self.assertIn('setOnErrorListener', java)
        self.assertIn('15000', java)
        self.assertIn('onPause()', java)
        self.assertIn('if !crate::android::intro_finished()', pages)
        self.assertIn('crate::android::first_frame_shown();', pages)
        self.assertNotIn('ui.draw_horizon_intro(', pages)
        self.assertIn('windowSplashScreenAnimatedIcon', source('packaging/android/res/values-v31/styles.xml'))

    def test_reveal_blocks_hits_and_respects_reduced_motion(self):
        pages = source('crates/horizon-client/src/android/launcher_pages.rs')
        self.assertIn('self.anim.reveal_start = Some', pages)
        self.assertIn('let item_progress = ((reveal - 0.24 - i as f32 * 0.045)', pages)
        self.assertIn('if reveal < 1.0 {\n            self.targets.clear();\n            self.list_targets.clear();', pages)
        self.assertIn('let reveal = if horizon_client::ui_theme::motion::reduced_motion() { 1.0 }', pages)
        self.assertIn('let from = [12.0, 98.0', pages)

    def test_touch_coalesces_cpu_work_and_invalidates_old_appearance(self):
        wardrobe = source('crates/horizon-client/src/android/wardrobe.rs')
        gesture = wardrobe.split('pub fn gesture(', 1)[1].split('    pub fn draw(', 1)[0]
        self.assertIn('self.queue_camera_render();', gesture)
        self.assertNotIn('self.preview();', gesture)
        self.assertIn('std::thread::spawn(move ||', wardrobe)
        self.assertIn('if self.render_worker.is_some() || !self.render_pending', wardrobe)
        self.assertIn('generation == self.appearance_generation', wardrobe)
        self.assertIn('self.poll_camera_render();', wardrobe)

if __name__ == '__main__':
    unittest.main()
