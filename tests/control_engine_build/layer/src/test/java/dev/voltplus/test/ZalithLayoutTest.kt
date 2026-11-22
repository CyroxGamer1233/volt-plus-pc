package dev.voltplus.test

import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.utils.layoutJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ZalithLayoutTest {
    private fun fixture(): String = checkNotNull(javaClass.classLoader)
        .getResourceAsStream("zalith-v11-fixture.json")!!.bufferedReader().use { it.readText() }

    @Test fun userLayoutLoadsAndRoundTripsThroughOriginalEngine() {
        val input = fixture()
        val parsed = loadLayoutFromString(input)
        assertEquals(12, parsed.editorVersion)
        assertEquals(5, parsed.layers.size)
        assertEquals(8, parsed.styles.size)
        assertEquals(listOf(1, 24, 0, 4, 10), parsed.layers.map { it.normalButtons.size })
        val switches = parsed.layers[0].normalButtons[0].clickEvents
        assertEquals(4, switches.size)
        assertTrue(switches.all { it.type == ClickEvent.Type.SwitchLayer })
        val diagonal = parsed.layers.last().normalButtons[0]
        assertTrue(diagonal.isSwipple)
        assertEquals(listOf("GLFW_KEY_W", "GLFW_KEY_A"), diagonal.clickEvents.map { it.key })
        val restored: ControlLayout = loadLayoutFromString(layoutJson.encodeToString(parsed))
        assertFalse(parsed.isModified(restored))
    }

    @Test fun newerUnsupportedVersionIsRejected() {
        val input = fixture().replace("\"editorVersion\": 11", "\"editorVersion\": 9999")
        assertNotEquals(fixture(), input)
        try {
            loadLayoutFromString(input)
            fail("A future layout version must not be treated as supported")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("not supported"))
        }
    }
}
