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
        actual = {str(p.relative_to(ROOT)) for root in ('third_party/pojav-control-engine', 'third_party/zalith-control-engine') for p in (ROOT / root).rglob('*') if p.is_file()}
        self.assertEqual(actual, set(manifest['files']))
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

    def test_supplied_pojav_and_zalith_input_events_can_be_translated(self):
        import re
        bridge = (ROOT / 'crates/horizon-client-input/src/java_controls.rs').read_text()
        entries = re.findall(r'\((\d+), "(GLFW_KEY_[A-Z_0-9]+)", "(key\.keyboard\.[a-z0-9.]+)"\)', bridge)
        codes = {int(code) for code, _, _ in entries}
        names = {name for _, name, _ in entries}
        pojav = json.loads((ROOT / 'tests/control_engine_build/fixtures/pojav-v4.json').read_text())
        self.assertEqual(pojav['version'], 4)
        self.assertEqual(len(pojav['mControlDataList']), 16)
        for button in pojav['mControlDataList']:
            for code in button['keycodes']:
                if code == 0: continue  # Pojav's unassigned slot
                if code < 0: self.assertIn(code, range(-9, 0))
                else: self.assertIn(code, codes)
        zalith = json.loads((ROOT / 'tests/control_engine_build/layer/src/test/resources/zalith-v11-fixture.json').read_text())
        layers = {layer['uuid'] for layer in zalith['layers']}
        launch = {'launcher.event.switch_ime', 'GLFW_MOUSE_BUTTON_LEFT', 'GLFW_MOUSE_BUTTON_RIGHT', 'GLFW_MOUSE_BUTTON_MIDDLE'}
        for layer in zalith['layers']:
            for button in layer.get('normalButtons', []):
                for event in button.get('clickEvents', []):
                    if event['type'] == 'key': self.assertIn(event['key'], names)
                    elif event['type'] in ('switch_layer', 'show_layer', 'hide_layer'):
                        self.assertIn(event['key'], layers)
                    elif event['type'] == 'launcher_event':
                        self.assertIn(event['key'], launch)
                    else: self.fail(f"Unknown fixture event: {event!r}")

    def test_layer_operations_and_special_actions_are_not_keys(self):
        bridge = (ROOT / 'crates/horizon-client-input/src/java_controls.rs').read_text()
        for name in ('ToggleKeyboard', 'ToggleControls', 'LayerSwitch', 'LayerShow', 'LayerHide', 'SendText'):
            self.assertIn(name, bridge)
        self.assertIn('release_all', bridge)
        self.assertIn('return None', bridge)

if __name__ == '__main__':
    unittest.main()
