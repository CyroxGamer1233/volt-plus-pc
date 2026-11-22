package dev.voltplus.test

import dev.horizon.android.control.ZalithStudioActions
import dev.horizon.android.control.ZalithStudioFiles
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class ZalithStudioFilesTest {
    @Test fun editWritesIndependentValidatedCopyAndPreservesOriginal() {
        val root = Files.createTempDirectory("studio-").toFile()
        try {
            val dir = root.resolve("controls/java/zalith").apply { mkdirs() }
            val original = dir.resolve("original.json")
            val bytes = javaClass.classLoader!!.getResourceAsStream("zalith-v11-fixture.json")!!.readBytes()
            original.writeBytes(bytes)
            val model = ZalithStudioFiles.load(original, root)
            val layer = model.layers.value.first()
            val added = ZalithStudioActions.addButton(layer)
            added.text.default = "My button"
            val saved = ZalithStudioFiles.saveCopy(original, root, model.pack())
            assertNotEquals(original, saved)
            assertArrayEquals(bytes, original.readBytes())
            val reloaded = ZalithStudioFiles.load(saved, root)
            assertEquals("My button", reloaded.layers.value.first().normalButtons.value.last().text.default)
            assertEquals(saved, ZalithStudioFiles.saveCopy(original, root, model.pack()))
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
        } finally { root.deleteRecursively() }
    }

    @Test fun refusesOutsideFilesAndOversizeInput() {
        val root = Files.createTempDirectory("studio-root-").toFile()
        val outside = Files.createTempFile("studio-outside-", ".json").toFile()
        try {
            try { ZalithStudioFiles.load(outside, root); fail("outside path") } catch (_: IllegalArgumentException) { }
            val file = root.resolve("controls/java/zalith/large.json")
            file.parentFile.mkdirs(); file.writeBytes(ByteArray(2 * 1024 * 1024 + 1))
            try { ZalithStudioFiles.load(file, root); fail("oversize") } catch (_: IllegalArgumentException) { }
        } finally { root.deleteRecursively(); outside.delete() }
    }
}
