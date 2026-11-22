package net.kdt.pojavlaunch.utils
object JSONUtils {
    @JvmStatic fun insertSingleJSONValue(expression: String, values: Map<String, String>): String {
        var result = expression
        values.forEach { (key, value) -> result = result.replace("\${$key}", value) }
        return result
    }
}
