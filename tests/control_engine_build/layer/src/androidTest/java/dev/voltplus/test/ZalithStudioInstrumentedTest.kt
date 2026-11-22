package dev.voltplus.test

import android.content.Intent
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.horizon.android.control.ZalithStudioActivity
import dev.horizon.android.control.ZalithStudioFiles
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the original editor canvas and a saved layout on an Android emulator. */
@RunWith(AndroidJUnit4::class)
class ZalithStudioInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun importEditAndSaveCopyFromActualStudioActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val dir = app.filesDir.resolve("controls/java/zalith").apply { mkdirs() }
        val original = dir.resolve("instrumentation-original.json")
        val bytes = instrumentation.context.assets.open("zalith-v11-fixture.json").use { it.readBytes() }
        original.writeBytes(bytes)
        try {
            val intent = Intent(app, ZalithStudioActivity::class.java)
                .putExtra("layout_path", original.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<ZalithStudioActivity>(intent).use {
                compose.onNodeWithText("Add button").assertExists().performClick()
                compose.onNodeWithText("Label").performTextClearance()
                compose.onNodeWithText("Label").performTextInput("Studio test")
                compose.onNodeWithText("Save copy").performClick()
                compose.waitUntil(timeoutMillis = 15000) {
                    dir.listFiles()?.any { f -> f.name.matches(Regex("[a-f0-9]{64}\\.json")) } == true
                }
                val saved = dir.listFiles()!!.single { f -> f.name.matches(Regex("[a-f0-9]{64}\\.json")) }
                assertArrayEquals(bytes, original.readBytes())
                val layout = ZalithStudioFiles.load(saved, app.filesDir)
                assertTrue(layout.layers.value.first().normalButtons.value.any { b -> b.text.default == "Studio test" })
            }
        } finally { dir.deleteRecursively() }
    }
}
