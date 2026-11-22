package dev.horizon.android.control

import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.event.EventHandler

/** Host operations. Implementations must be invoked on the Android UI thread. */
interface ControlInputSink {
    /** Returns false when a particular input is unsupported. */
    fun input(type: String, key: String, pressed: Boolean): Boolean
    fun toggleKeyboard()
    fun toggleControls()
    fun toggleMenu()
    fun sendText(text: String)
    fun reset()
    fun unsupported(type: String, key: String)
}

interface Repeater {
    fun postDelayed(task: Runnable, delayMillis: Long)
    fun remove(task: Runnable)
}

/**
 * Adapts the original LayerController's events. The original engine applies
 * layer visibility operations itself. Long-scroll repeats until all holders
 * release; disposing the overlay releases all native keys and repeat timers.
 */
class ZalithEvents(private val sink: ControlInputSink, private val repeater: Repeater) {
    private var upHolders = 0
    private var downHolders = 0
    private val upTask = repeatingTask(true)
    private val downTask = repeatingTask(false)
    val handler = EventHandler(::accept)

    private fun repeatingTask(up: Boolean): Runnable = object : Runnable {
        override fun run() {
            if ((if (up) upHolders else downHolders) <= 0) return
            val name = if (up) "launcher.event.scroll_up.single" else "launcher.event.scroll_down.single"
            emit("launcher_event", name, true)
            emit("launcher_event", name, false)
            repeater.postDelayed(this, 80)
        }
    }

    private fun emit(type: String, key: String, pressed: Boolean) {
        if (!sink.input(type, key, pressed)) sink.unsupported(type, key)
    }

    private fun heldScroll(up: Boolean, pressed: Boolean) {
        val old = if (up) upHolders else downHolders
        val count = if (pressed) (old + 1).coerceAtMost(128) else (old - 1).coerceAtLeast(0)
        if (up) upHolders = count else downHolders = count
        val task = if (up) upTask else downTask
        if (old == 0 && count > 0) task.run()
        if (old > 0 && count == 0) repeater.remove(task)
    }

    fun accept(event: ClickEvent, pressed: Boolean) {
        when (event.type) {
            ClickEvent.Type.Key -> emit("key", event.key, pressed)
            ClickEvent.Type.LauncherEvent -> when (event.key) {
                "launcher.event.switch_ime" -> if (pressed) sink.toggleKeyboard()
                "launcher.event.switch_menu" -> if (pressed) sink.toggleMenu()
                "launcher.event.scroll_up" -> heldScroll(true, pressed)
                "launcher.event.scroll_down" -> heldScroll(false, pressed)
                "launcher.event.scroll_up.single", "launcher.event.scroll_down.single" ->
                    if (pressed) { emit("launcher_event", event.key, true); emit("launcher_event", event.key, false) }
                else -> emit("launcher_event", event.key, pressed)
            }
            ClickEvent.Type.SendText -> if (pressed) sink.sendText(event.key)
            // The upstream control processor handles these before the callback.
            ClickEvent.Type.SwitchLayer, ClickEvent.Type.ShowLayer, ClickEvent.Type.HideLayer -> Unit
        }
    }

    fun dispose() {
        upHolders = 0
        downHolders = 0
        repeater.remove(upTask)
        repeater.remove(downTask)
        sink.reset()
    }
}
