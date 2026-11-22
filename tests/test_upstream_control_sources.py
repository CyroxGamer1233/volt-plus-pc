"""Guard the provenance and Java exception for the upstream control sources."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class UpstreamControlSources(unittest.TestCase):
    def test_sources_match_pinned_checksums(self):
        manifest = json.loads((ROOT / 'tools/upstream_control_sources.json').read_text())
        self.assertGreater(len(manifest['files']), 100)
        for path, digest in manifest['files'].items():
            self.assertEqual(hashlib.sha256((ROOT / path).read_bytes()).hexdigest(), digest, path)

    def test_java_allowlist_is_exact(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location('tree_check', ROOT / 'tools/check_public_tree.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        for name in module.UPSTREAM_CONTROL_JAVA:
            self.assertFalse(module.prohibited(name), name)
        self.assertTrue(module.prohibited('third_party/pojav-control-engine/src/customcontrols/RecoveredGame.java'))
        self.assertTrue(module.prohibited('third_party/zalith-control-engine/InputMap/RecoveredGame.java'))
        self.assertTrue(module.prohibited('third_party/pojav-control-engine/src/customcontrols/Injected.class'))

if __name__ == '__main__':
    unittest.main()

class InputTranslationCoverage(unittest.TestCase):
    def test_known_glfw_codes_match_available_tokens(self):
        import re
        bridge = (ROOT / 'crates/horizon-client-input/src/java_controls.rs').read_text()
        binds = (ROOT / 'crates/horizon-client-input/src/keybinds.rs').read_text()
        entries = re.findall(r'\((\d+), "(GLFW_KEY_[A-Z_0-9]+)", "(key\.keyboard\.[a-z0-9.]+)"\)', bridge)
        self.assertGreater(len(entries), 100)
        self.assertEqual(len({n for n, _, _ in entries}), len(entries))
        self.assertEqual(len({name for _, name, _ in entries}), len(entries))
        for _, _, token in entries:
            self.assertIn(f'"{token}"', binds)
        for code, name, token in [('87', 'GLFW_KEY_W', 'key.keyboard.w'),
                                  ('292', 'GLFW_KEY_F3', 'key.keyboard.f3'),
                                  ('340', 'GLFW_KEY_LEFT_SHIFT', 'key.keyboard.left.shift')]:
            self.assertIn((code, name, token), entries)
        self.assertNotIn('GLFW_KEY_UNKNOWN', {name for _, name, _ in entries})

    def test_layer_operations_and_special_actions_are_not_keys(self):
        bridge = (ROOT / 'crates/horizon-client-input/src/java_controls.rs').read_text()
        for name in ('ToggleKeyboard', 'ToggleControls', 'LayerSwitch', 'LayerShow', 'LayerHide', 'SendText'):
            self.assertIn(name, bridge)
        self.assertIn('release_all', bridge)
        self.assertIn('return None', bridge)
