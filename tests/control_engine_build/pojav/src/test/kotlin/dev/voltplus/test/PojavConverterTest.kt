package dev.voltplus.test

import net.kdt.pojavlaunch.customcontrols.LayoutConverter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    @Test fun futureVersionIsRejectedByOriginalConverter() {
        val file = Files.createTempFile("pojav-future-", ".json").toFile()
        try {
            file.writeText("{\"version\":999,\"mControlDataList\":[]}")
            assertNull(LayoutConverter.loadAndConvertIfNecessary(file.path))
        } finally { file.delete() }
    }
}
