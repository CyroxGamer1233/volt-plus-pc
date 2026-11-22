package dev.voltplus.test

import com.movtery.layer_controller.event.ClickEvent
import dev.horizon.android.control.ControlInputSink
import dev.horizon.android.control.Repeater
import dev.horizon.android.control.ZalithEvents
import org.junit.Assert.*
import org.junit.Test

class ZalithEventsTest {
    private class Sink : ControlInputSink {
        val edges = mutableListOf<Triple<String, String, Boolean>>()
        val unsupported = mutableListOf<Pair<String, String>>()
        var keyboard = 0
        var menu = 0
        var text: String? = null
        var resets = 0
        override fun input(type: String, key: String, pressed: Boolean): Boolean {
            edges.add(Triple(type, key, pressed))
            return key != "unknown"
        }
        override fun toggleKeyboard() { keyboard++ }
        override fun toggleMenu() { menu++ }
        override fun sendText(text: String) { this.text = text }
        override fun reset() { resets++ }
        override fun unsupported(type: String, key: String) { unsupported.add(type to key) }
    }
    private class FakeRepeater : Repeater {
        val pending = mutableSetOf<Runnable>()
        override fun postDelayed(task: Runnable, delayMillis: Long) { pending.add(task) }
        override fun remove(task: Runnable) { pending.remove(task) }
        fun tick() { val jobs = pending.toList(); pending.clear(); jobs.forEach { it.run() } }
    }
    private fun key(name: String) = ClickEvent(ClickEvent.Type.Key, name)
    private fun launch(name: String) = ClickEvent(ClickEvent.Type.LauncherEvent, name)

    @Test fun forwardsEdgesAndDoesNotDoubleHandleLayerEvents() {
        val sink = Sink(); val r = FakeRepeater(); val a = ZalithEvents(sink, r)
        a.accept(key("GLFW_KEY_W"), true); a.accept(key("GLFW_KEY_W"), false)
        assertEquals(listOf(Triple("key", "GLFW_KEY_W", true), Triple("key", "GLFW_KEY_W", false)), sink.edges)
        a.accept(ClickEvent(ClickEvent.Type.SwitchLayer, "target"), true)
        assertEquals(2, sink.edges.size)
        a.accept(launch("launcher.event.switch_ime"), true)
        a.accept(launch("launcher.event.switch_ime"), false)
        assertEquals(1, sink.keyboard)
        a.accept(key("unknown"), true)
        assertEquals(listOf("key" to "unknown"), sink.unsupported)
        a.accept(ClickEvent(ClickEvent.Type.SendText, "hello"), true)
        assertEquals("hello", sink.text)
        a.dispose(); assertEquals(1, sink.resets)
    }

    @Test fun heldScrollRepeatsAndStopsOnLastReleaseOrDispose() {
        val sink = Sink(); val r = FakeRepeater(); val a = ZalithEvents(sink, r)
        val scroll = launch("launcher.event.scroll_up")
        a.accept(scroll, true); a.accept(scroll, true)
        assertEquals(2, sink.edges.size)
        r.tick(); assertEquals(4, sink.edges.size)
        a.accept(scroll, false); r.tick(); assertEquals(6, sink.edges.size)
        a.accept(scroll, false); r.tick(); assertEquals(6, sink.edges.size)
        a.accept(scroll, true); a.dispose(); r.tick()
        assertEquals(8, sink.edges.size)
        assertEquals(1, sink.resets)
    }
}
