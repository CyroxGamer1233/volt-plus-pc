package dev.horizon.android.control

import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromString
import com.movtery.layer_controller.observable.ObservableControlLayout
import com.movtery.layer_controller.utils.layoutJson
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest

/** Safely reads original imports and writes edited copies, never over originals. */
object ZalithStudioFiles {
    private const val LIMIT = 2 * 1024 * 1024

    private fun checked(source: File, privateRoot: File): File {
        val root = privateRoot.canonicalFile
        val file = source.canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "Layout is outside app storage" }
        require(source.absoluteFile.normalize() == file) { "Redirected layout paths are not allowed" }
        require(file.parentFile?.name == "zalith" && file.parentFile?.parentFile?.name == "java" && file.parentFile?.parentFile?.parentFile?.name == "controls") { "Not a saved Zalith layout" }
        require(file.isFile && file.name.endsWith(".json")) { "Layout file is unavailable" }
        return file
    }

    /** Enumerate only validated regular paths next to the imported file. */
    fun listLayouts(source: File, privateRoot: File): List<File> {
        val file = checked(source, privateRoot)
        return file.parentFile?.listFiles().orEmpty().asSequence()
            .filter { it.name.endsWith(".json") }
            .mapNotNull { candidate ->
                try { checked(candidate, privateRoot).takeIf { it.length() <= LIMIT } }
                catch (_: Exception) { null }
            }
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenBy { it.name })
            .take(200).toList()
    }

    fun load(source: File, privateRoot: File): ObservableControlLayout {
        val file = checked(source, privateRoot)
        val bytes = file.inputStream().use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() <= LIMIT) {
                val count = input.read(buffer, 0, minOf(buffer.size, LIMIT + 1 - out.size()))
                if (count < 0) break
                if (count == 0) continue
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        }
        require(bytes.size <= LIMIT) { "Control layout exceeds 2 MiB" }
        return ObservableControlLayout(loadLayoutFromString(bytes.toString(Charsets.UTF_8)))
    }

    fun saveCopy(source: File, privateRoot: File, layout: ControlLayout): File {
        val original = checked(source, privateRoot)
        val bytes = layoutJson.encodeToString(layout).toByteArray(Charsets.UTF_8)
        require(bytes.size <= LIMIT) { "Edited control layout exceeds 2 MiB" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val target = File(original.parentFile, "$digest.json")
        val temp = File.createTempFile(".studio-", ".tmp", original.parentFile)
        try {
            FileOutputStream(temp).use { out -> out.write(bytes); out.fd.sync() }
            try {
                Files.createLink(target.toPath(), temp.toPath())
            } catch (e: java.nio.file.FileAlreadyExistsException) {
                if (Files.isSymbolicLink(target.toPath()) || !target.isFile || target.length() != bytes.size.toLong() || !target.readBytes().contentEquals(bytes)) {
                    throw IOException("Existing edited layout does not match", e)
                }
            }
            return target
        } finally {
            temp.delete()
        }
    }
}
