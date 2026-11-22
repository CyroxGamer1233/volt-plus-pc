package dev.horizon.android.control

import net.kdt.pojavlaunch.customcontrols.CustomControls
import net.kdt.pojavlaunch.customcontrols.LayoutConverter
import java.io.File
import java.io.IOException

/**
 * Pojav's v1/v2 converter saves migrations to its input path. Always provide a
 * private temporary copy: imported originals must remain byte-for-byte intact.
 * This is an adapter for the upstream converter, not a control-view host.
 */
object SafePojavLoader {
    private const val MAX_BYTES = 2 * 1024 * 1024

    @JvmStatic
    fun load(bytes: ByteArray, cacheDirectory: File): CustomControls {
        require(bytes.size <= MAX_BYTES) { "Control layout exceeds 2 MiB" }
        require(cacheDirectory.isDirectory) { "Private control cache is unavailable" }
        val temporary = File.createTempFile("pojav-layout-", ".json", cacheDirectory)
        try {
            temporary.outputStream().use { it.write(bytes) }
            val converted = LayoutConverter.loadAndConvertIfNecessary(temporary.absolutePath)
                ?: throw IOException("Unsupported Pojav control layout version")
            requireNotNull(converted.mControlDataList) { "Missing Pojav control list" }
            requireNotNull(converted.mDrawerDataList) { "Missing Pojav drawer list" }
            requireNotNull(converted.mJoystickDataList) { "Missing Pojav joystick list" }
            return converted
        } finally {
            temporary.delete()
        }
    }
}
