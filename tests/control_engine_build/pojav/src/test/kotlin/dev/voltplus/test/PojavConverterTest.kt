package dev.voltplus.test

import net.kdt.pojavlaunch.customcontrols.LayoutConverter
import dev.horizon.android.control.SafePojavLoader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.lwjgl.glfw.CallbackBridge
import java.nio.file.Files

/** The actual upstream converter and data classes run against test host stubs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PojavConverterTest {
    @Test fun suppliedV4LayoutConvertsWithoutChangingTheOriginal() {
        val original = javaClass.classLoader!!.getResourceAsStream("pojav-v4.json")!!.readBytes()
        val file = Files.createTempFile("pojav-layout-", ".json").toFile()
        try {
            file.writeBytes(original)
            val converted = LayoutConverter.loadAndConvertIfNecessary(file.path)
            assertNotNull(converted)
            assertEquals(6, converted.version)
            assertEquals(16, converted.mControlDataList.size)
            assertEquals(0, converted.mDrawerDataList.size)
            assertArrayEquals(original, file.readBytes())
            assertTrue(converted.mControlDataList.any { it.keycodes.contains(87) })
        } finally { file.delete() }
    }

    @Test fun suppliedDynamicPositionsUseActualPojavExpressionEngine() {
        val original = javaClass.classLoader!!.getResourceAsStream("pojav-v4.json")!!.readBytes()
        val cache = Files.createTempDirectory("pojav-positions-").toFile()
        val oldWidth = CallbackBridge.physicalWidth
        val oldHeight = CallbackBridge.physicalHeight
        try {
            val converted = SafePojavLoader.load(original, cache)
            val mouse = converted.mControlDataList.first { it.name == "Mouse" }
            for ((width, height) in listOf(1920 to 1080, 1280 to 720)) {
                CallbackBridge.physicalWidth = width
                CallbackBridge.physicalHeight = height
                for (button in converted.mControlDataList) {
                    assertTrue("x for ${button.name}", button.insertDynamicPos(button.dynamicX).isFinite())
                    assertTrue("y for ${button.name}", button.insertDynamicPos(button.dynamicY).isFinite())
                }
                assertEquals(width.toFloat() - mouse.width, mouse.insertDynamicPos(mouse.dynamicX), 0.01f)
            }
        } finally {
            CallbackBridge.physicalWidth = oldWidth
            CallbackBridge.physicalHeight = oldHeight
            cache.deleteRecursively()
        }
    }

    @Test fun adapterMigratesScratchCopyAndLeavesOriginalUntouched() {
        val original = javaClass.classLoader!!.getResourceAsStream("pojav-v4.json")!!.readBytes()
        // Version 2 triggers the upstream converter's in-place save.
        val v2 = String(original).replace("\"version\":4", "\"version\":2").toByteArray()
        assertFalse(v2.contentEquals(original))
        val before = v2.copyOf()
        val cache = Files.createTempDirectory("pojav-safe-").toFile()
        try {
            val converted = SafePojavLoader.load(v2, cache)
            assertEquals(8, converted.version)
            assertEquals(16, converted.mControlDataList.size)
            assertArrayEquals(before, v2)
            assertEquals(0, cache.listFiles()!!.size)
        } finally { cache.deleteRecursively() }
    }

    @Test fun adapterCleansUpAfterFailure() {
        val cache = Files.createTempDirectory("pojav-bad-").toFile()
        try {
            try {
                SafePojavLoader.load("{\"version\":999}".toByteArray(), cache)
                fail("Unknown version should be rejected")
            } catch (_: java.io.IOException) { }
            assertEquals(0, cache.listFiles()!!.size)
        } finally { cache.deleteRecursively() }
    }

    @Test fun futureVersionIsRejectedByOriginalConverter() {
        val file = Files.createTempFile("pojav-future-", ".json").toFile()
        try {
            file.writeText("{\"version\":999,\"mControlDataList\":[]}")
            assertNull(LayoutConverter.loadAndConvertIfNecessary(file.path))
        } finally { file.delete() }
    }
}
