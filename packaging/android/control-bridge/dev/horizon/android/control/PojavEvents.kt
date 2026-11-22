package dev.horizon.android.control

/** Adapter for callbacks from Pojav's original control views.
 * The native bridge owns keyboard/mouse translation and wheel edge semantics;
 * host-only actions are handled on the UI thread, not treated as GLFW keys.
 */
class PojavEvents(private val sink: ControlInputSink) {
    fun accept(code: Int, pressed: Boolean) {
        when (code) {
            0 -> Unit // Pojav's GLFW_KEY_UNKNOWN / unassigned slot
            -1 -> if (pressed) sink.toggleKeyboard()
            -2 -> if (pressed) sink.toggleControls()
            -9 -> if (pressed) sink.toggleMenu()
            else -> if (!sink.input("pojav", code.toString(), pressed)) {
                sink.unsupported("pojav", code.toString())
            }
        }
    }

    fun dispose() = sink.reset()
}
