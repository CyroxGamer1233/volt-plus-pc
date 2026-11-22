// Test-only Pojav host stubs. These do not implement Android controls.
package net.kdt.pojavlaunch

import android.util.DisplayMetrics
import com.google.gson.Gson
import java.io.File

object Tools {
    @JvmField val GLOBAL_GSON = Gson()
    @JvmField val currentDisplayMetrics = DisplayMetrics().apply { widthPixels = 1920; heightPixels = 1080 }
    @JvmStatic fun pxToDp(value: Float) = value
    @JvmStatic fun dpToPx(value: Float) = value
    @JvmStatic fun isValidString(value: String?) = !value.isNullOrEmpty()
    @JvmStatic fun read(path: String): String = File(path).readText()
    @JvmStatic fun write(path: String, contents: String) = File(path).writeText(contents)
}
object LwjglGlfwKeycode {
    const val GLFW_KEY_UNKNOWN = 0
    const val GLFW_KEY_SPACE = 32
    const val GLFW_KEY_A = 65
    const val GLFW_KEY_D = 68
    const val GLFW_KEY_E = 69
    const val GLFW_KEY_S = 83
    const val GLFW_KEY_T = 84
    const val GLFW_KEY_W = 87
    const val GLFW_KEY_TAB = 258
    const val GLFW_KEY_F3 = 292
    const val GLFW_KEY_F5 = 294
    const val GLFW_KEY_LEFT_SHIFT = 340
    const val GLFW_KEY_LEFT_CONTROL = 341
    const val GLFW_KEY_LEFT_ALT = 342
}
object R {
    object string {
        const val control_debug = 1
        const val control_chat = 2
        const val control_listplayers = 3
        const val control_thirdperson = 4
        const val control_up = 5
        const val control_left = 6
        const val control_down = 7
        const val control_right = 8
        const val control_inventory = 9
        const val control_shift = 10
        const val control_jump = 11
    }
}
