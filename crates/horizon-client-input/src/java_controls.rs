//! Shared input translation for the Pojav and Zalith Android control adapters.
//! These helpers do not provide an overlay or import layouts by themselves.
use std::collections::{BTreeMap, BTreeSet};

/// The GLFW keyboard identifiers used by both upstream launchers. Unknown keys
/// are rejected, not interpreted as an unassigned or unrelated game control.
const GLFW_KEYS: &[(i32, &str, &str)] = &[
    (32, "GLFW_KEY_SPACE", "key.keyboard.space"),
    (39, "GLFW_KEY_APOSTROPHE", "key.keyboard.apostrophe"),
    (44, "GLFW_KEY_COMMA", "key.keyboard.comma"),
    (45, "GLFW_KEY_MINUS", "key.keyboard.minus"),
    (46, "GLFW_KEY_PERIOD", "key.keyboard.period"),
    (47, "GLFW_KEY_SLASH", "key.keyboard.slash"),
    (48, "GLFW_KEY_0", "key.keyboard.0"),
    (49, "GLFW_KEY_1", "key.keyboard.1"),
    (50, "GLFW_KEY_2", "key.keyboard.2"),
    (51, "GLFW_KEY_3", "key.keyboard.3"),
    (52, "GLFW_KEY_4", "key.keyboard.4"),
    (53, "GLFW_KEY_5", "key.keyboard.5"),
    (54, "GLFW_KEY_6", "key.keyboard.6"),
    (55, "GLFW_KEY_7", "key.keyboard.7"),
    (56, "GLFW_KEY_8", "key.keyboard.8"),
    (57, "GLFW_KEY_9", "key.keyboard.9"),
    (59, "GLFW_KEY_SEMICOLON", "key.keyboard.semicolon"),
    (61, "GLFW_KEY_EQUAL", "key.keyboard.equal"),
    (65, "GLFW_KEY_A", "key.keyboard.a"),
    (66, "GLFW_KEY_B", "key.keyboard.b"),
    (67, "GLFW_KEY_C", "key.keyboard.c"),
    (68, "GLFW_KEY_D", "key.keyboard.d"),
    (69, "GLFW_KEY_E", "key.keyboard.e"),
    (70, "GLFW_KEY_F", "key.keyboard.f"),
    (71, "GLFW_KEY_G", "key.keyboard.g"),
    (72, "GLFW_KEY_H", "key.keyboard.h"),
    (73, "GLFW_KEY_I", "key.keyboard.i"),
    (74, "GLFW_KEY_J", "key.keyboard.j"),
    (75, "GLFW_KEY_K", "key.keyboard.k"),
    (76, "GLFW_KEY_L", "key.keyboard.l"),
    (77, "GLFW_KEY_M", "key.keyboard.m"),
    (78, "GLFW_KEY_N", "key.keyboard.n"),
    (79, "GLFW_KEY_O", "key.keyboard.o"),
    (80, "GLFW_KEY_P", "key.keyboard.p"),
    (81, "GLFW_KEY_Q", "key.keyboard.q"),
    (82, "GLFW_KEY_R", "key.keyboard.r"),
    (83, "GLFW_KEY_S", "key.keyboard.s"),
    (84, "GLFW_KEY_T", "key.keyboard.t"),
    (85, "GLFW_KEY_U", "key.keyboard.u"),
    (86, "GLFW_KEY_V", "key.keyboard.v"),
    (87, "GLFW_KEY_W", "key.keyboard.w"),
    (88, "GLFW_KEY_X", "key.keyboard.x"),
    (89, "GLFW_KEY_Y", "key.keyboard.y"),
    (90, "GLFW_KEY_Z", "key.keyboard.z"),
    (91, "GLFW_KEY_LEFT_BRACKET", "key.keyboard.left.bracket"),
    (92, "GLFW_KEY_BACKSLASH", "key.keyboard.backslash"),
    (93, "GLFW_KEY_RIGHT_BRACKET", "key.keyboard.right.bracket"),
    (96, "GLFW_KEY_GRAVE_ACCENT", "key.keyboard.grave.accent"),
    (161, "GLFW_KEY_WORLD_1", "key.keyboard.world.1"),
    (256, "GLFW_KEY_ESCAPE", "key.keyboard.escape"),
    (257, "GLFW_KEY_ENTER", "key.keyboard.enter"),
    (258, "GLFW_KEY_TAB", "key.keyboard.tab"),
    (259, "GLFW_KEY_BACKSPACE", "key.keyboard.backspace"),
    (260, "GLFW_KEY_INSERT", "key.keyboard.insert"),
    (261, "GLFW_KEY_DELETE", "key.keyboard.delete"),
    (262, "GLFW_KEY_RIGHT", "key.keyboard.right"),
    (263, "GLFW_KEY_LEFT", "key.keyboard.left"),
    (264, "GLFW_KEY_DOWN", "key.keyboard.down"),
    (265, "GLFW_KEY_UP", "key.keyboard.up"),
    (266, "GLFW_KEY_PAGE_UP", "key.keyboard.page.up"),
    (267, "GLFW_KEY_PAGE_DOWN", "key.keyboard.page.down"),
    (268, "GLFW_KEY_HOME", "key.keyboard.home"),
    (269, "GLFW_KEY_END", "key.keyboard.end"),
    (280, "GLFW_KEY_CAPS_LOCK", "key.keyboard.caps.lock"),
    (281, "GLFW_KEY_SCROLL_LOCK", "key.keyboard.scroll.lock"),
    (282, "GLFW_KEY_NUM_LOCK", "key.keyboard.num.lock"),
    (283, "GLFW_KEY_PRINT_SCREEN", "key.keyboard.print.screen"),
    (284, "GLFW_KEY_PAUSE", "key.keyboard.pause"),
    (290, "GLFW_KEY_F1", "key.keyboard.f1"),
    (291, "GLFW_KEY_F2", "key.keyboard.f2"),
    (292, "GLFW_KEY_F3", "key.keyboard.f3"),
    (293, "GLFW_KEY_F4", "key.keyboard.f4"),
    (294, "GLFW_KEY_F5", "key.keyboard.f5"),
    (295, "GLFW_KEY_F6", "key.keyboard.f6"),
    (296, "GLFW_KEY_F7", "key.keyboard.f7"),
    (297, "GLFW_KEY_F8", "key.keyboard.f8"),
    (298, "GLFW_KEY_F9", "key.keyboard.f9"),
    (299, "GLFW_KEY_F10", "key.keyboard.f10"),
    (300, "GLFW_KEY_F11", "key.keyboard.f11"),
    (301, "GLFW_KEY_F12", "key.keyboard.f12"),
    (302, "GLFW_KEY_F13", "key.keyboard.f13"),
    (303, "GLFW_KEY_F14", "key.keyboard.f14"),
    (304, "GLFW_KEY_F15", "key.keyboard.f15"),
    (305, "GLFW_KEY_F16", "key.keyboard.f16"),
    (306, "GLFW_KEY_F17", "key.keyboard.f17"),
    (307, "GLFW_KEY_F18", "key.keyboard.f18"),
    (308, "GLFW_KEY_F19", "key.keyboard.f19"),
    (309, "GLFW_KEY_F20", "key.keyboard.f20"),
    (310, "GLFW_KEY_F21", "key.keyboard.f21"),
    (311, "GLFW_KEY_F22", "key.keyboard.f22"),
    (312, "GLFW_KEY_F23", "key.keyboard.f23"),
    (313, "GLFW_KEY_F24", "key.keyboard.f24"),
    (320, "GLFW_KEY_KP_0", "key.keyboard.keypad.0"),
    (321, "GLFW_KEY_KP_1", "key.keyboard.keypad.1"),
    (322, "GLFW_KEY_KP_2", "key.keyboard.keypad.2"),
    (323, "GLFW_KEY_KP_3", "key.keyboard.keypad.3"),
    (324, "GLFW_KEY_KP_4", "key.keyboard.keypad.4"),
    (325, "GLFW_KEY_KP_5", "key.keyboard.keypad.5"),
    (326, "GLFW_KEY_KP_6", "key.keyboard.keypad.6"),
    (327, "GLFW_KEY_KP_7", "key.keyboard.keypad.7"),
    (328, "GLFW_KEY_KP_8", "key.keyboard.keypad.8"),
    (329, "GLFW_KEY_KP_9", "key.keyboard.keypad.9"),
    (330, "GLFW_KEY_KP_DECIMAL", "key.keyboard.keypad.period"),
    (331, "GLFW_KEY_KP_DIVIDE", "key.keyboard.keypad.divide"),
    (332, "GLFW_KEY_KP_MULTIPLY", "key.keyboard.keypad.multiply"),
    (333, "GLFW_KEY_KP_SUBTRACT", "key.keyboard.keypad.subtract"),
    (334, "GLFW_KEY_KP_ADD", "key.keyboard.keypad.add"),
    (335, "GLFW_KEY_KP_ENTER", "key.keyboard.keypad.enter"),
    (336, "GLFW_KEY_KP_EQUAL", "key.keyboard.keypad.equal"),
    (340, "GLFW_KEY_LEFT_SHIFT", "key.keyboard.left.shift"),
    (341, "GLFW_KEY_LEFT_CONTROL", "key.keyboard.left.control"),
    (342, "GLFW_KEY_LEFT_ALT", "key.keyboard.left.alt"),
    (343, "GLFW_KEY_LEFT_SUPER", "key.keyboard.left.win"),
    (344, "GLFW_KEY_RIGHT_SHIFT", "key.keyboard.right.shift"),
    (345, "GLFW_KEY_RIGHT_CONTROL", "key.keyboard.right.control"),
    (346, "GLFW_KEY_RIGHT_ALT", "key.keyboard.right.alt"),
    (347, "GLFW_KEY_RIGHT_SUPER", "key.keyboard.right.win"),
    (348, "GLFW_KEY_MENU", "key.keyboard.menu"),
];

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ControlEvent {
    Token(&'static str),
    ToggleKeyboard,
    ToggleControls,
    OpenMenu,
    ScrollUpHeld,
    ScrollDownHeld,
    LayerSwitch,
    LayerShow,
    LayerHide,
    SendText,
}

pub fn glfw_key(code: i32) -> Option<&'static str> {
    GLFW_KEYS.iter().find(|entry| entry.0 == code).map(|entry| entry.2)
}
pub fn glfw_key_named(name: &str) -> Option<&'static str> {
    GLFW_KEYS.iter().find(|entry| entry.1 == name).map(|entry| entry.2)
}
/// Pojav reserves negative codes for launcher actions, distinct from GLFW keys.
pub fn pojav_code(code: i32) -> Option<ControlEvent> {
    Some(match code {
        -1 => ControlEvent::ToggleKeyboard,
        -2 => ControlEvent::ToggleControls,
        -3 => ControlEvent::Token("key.mouse.left"),
        -4 => ControlEvent::Token("key.mouse.right"),
        -5 => ControlEvent::Token("mouse.virtual.toggle"),
        -6 => ControlEvent::Token("key.mouse.middle"),
        -7 => ControlEvent::Token("mouse.scroll.up"),
        -8 => ControlEvent::Token("mouse.scroll.down"),
        -9 => ControlEvent::OpenMenu,
        _ => ControlEvent::Token(glfw_key(code)?),
    })
}
/// Layer operations and text keep their original payload with the caller.
/// Unknown launcher-specific actions must be surfaced, never dropped silently.
pub fn zalith_event(kind: &str, key: &str) -> Option<ControlEvent> {
    match kind {
        "key" => glfw_key_named(key).map(ControlEvent::Token),
        "switch_layer" => Some(ControlEvent::LayerSwitch),
        "show_layer" => Some(ControlEvent::LayerShow),
        "hide_layer" => Some(ControlEvent::LayerHide),
        "send_text" => Some(ControlEvent::SendText),
        "launcher_event" => Some(match key {
            "GLFW_MOUSE_BUTTON_LEFT" => ControlEvent::Token("key.mouse.left"),
            "GLFW_MOUSE_BUTTON_RIGHT" => ControlEvent::Token("key.mouse.right"),
            "GLFW_MOUSE_BUTTON_MIDDLE" => ControlEvent::Token("key.mouse.middle"),
            "launcher.event.switch_ime" => ControlEvent::ToggleKeyboard,
            "launcher.event.switch_menu" => ControlEvent::OpenMenu,
            "launcher.event.scroll_up" => ControlEvent::ScrollUpHeld,
            "launcher.event.scroll_down" => ControlEvent::ScrollDownHeld,
            "launcher.event.scroll_up.single" => ControlEvent::Token("mouse.scroll.up"),
            "launcher.event.scroll_down.single" => ControlEvent::Token("mouse.scroll.down"),
            _ => return None,
        }),
        _ => None,
    }
}

/// Tracks per-widget/finger ownership. Releasing one of several controls bound
/// to the same key must not release that key until the last owner lets go.
#[derive(Default)]
pub struct HoldTracker {
    by_owner: BTreeMap<u64, BTreeSet<String>>,
    counts: BTreeMap<String, usize>,
}
impl HoldTracker {
    pub fn press(&mut self, owner: u64, token: &str) -> Option<(String, bool)> {
        if !self.by_owner.entry(owner).or_default().insert(token.to_owned()) { return None; }
        let count = self.counts.entry(token.to_owned()).or_default();
        *count += 1;
        (*count == 1).then(|| (token.to_owned(), true))
    }
    pub fn release(&mut self, owner: u64) -> Vec<(String, bool)> {
        let mut events = Vec::new();
        if let Some(tokens) = self.by_owner.remove(&owner) {
            for token in tokens {
                if let Some(count) = self.counts.get_mut(&token) {
                    *count -= 1;
                    if *count == 0 {
                        self.counts.remove(&token);
                        events.push((token, false));
                    }
                }
            }
        }
        events
    }
    /// Use on focus loss, overlay shutdown and scene changes to prevent stuck keys.
    pub fn release_all(&mut self) -> Vec<(String, bool)> {
        self.by_owner.clear();
        std::mem::take(&mut self.counts).into_keys().map(|token| (token, false)).collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn mapped_keys_are_known_and_both_forms_agree() {
        for &(code, name, token) in GLFW_KEYS {
            assert_eq!(glfw_key(code), Some(token));
            assert_eq!(glfw_key_named(name), Some(token));
            assert!(crate::keybinds::key_code(token).is_some(), "{token}");
        }
        assert_eq!(glfw_key(9999), None);
        assert_eq!(glfw_key_named("GLFW_KEY_F25"), None);
    }
    #[test]
    fn launcher_actions_are_not_confused_with_keys() {
        assert_eq!(pojav_code(-1), Some(ControlEvent::ToggleKeyboard));
        assert_eq!(pojav_code(-3), Some(ControlEvent::Token("key.mouse.left")));
        assert_eq!(pojav_code(87), Some(ControlEvent::Token("key.keyboard.w")));
        assert_eq!(zalith_event("switch_layer", "layer-id"), Some(ControlEvent::LayerSwitch));
        assert_eq!(zalith_event("launcher_event", "launcher.event.scroll_up"), Some(ControlEvent::ScrollUpHeld));
        assert_eq!(zalith_event("launcher_event", "launcher.event.scroll_up.single"), Some(ControlEvent::Token("mouse.scroll.up")));
        assert_eq!(zalith_event("launcher_event", "unimplemented"), None);
    }
    #[test]
    fn overlapping_presses_release_only_after_last_owner() {
        let mut t = HoldTracker::default();
        assert_eq!(t.press(1, "w"), Some(("w".into(), true)));
        assert_eq!(t.press(1, "w"), None);
        assert_eq!(t.press(2, "w"), None);
        assert!(t.release(1).is_empty());
        assert_eq!(t.release(2), vec![("w".into(), false)]);
        assert_eq!(t.press(3, "s"), Some(("s".into(), true)));
        assert_eq!(t.release_all(), vec![("s".into(), false)]);
        assert!(t.release(3).is_empty());
    }
}
