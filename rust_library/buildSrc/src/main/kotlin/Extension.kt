import org.gradle.api.Project
import java.util.Properties

private val Project.propertiesMap by lazy { mutableMapOf<String, Properties>() }
fun Project.localProperties(filename: String = "local.properties"): Properties {
  return propertiesMap.getOrPut(filename) {
    Properties().also { properties ->
      rootDir.resolve("local.properties").apply {
        if (exists()) {
          inputStream().use { properties.load(it) }
        }
      }
    }
  }
}
fun Properties.getBoolean(key: String) = this[key] == "true"
fun Properties.getString(key: String) =
  (this[key] ?: throw Exception("properties key not found: $key")).toString()

fun Properties.getStringOrNull(key: String) = this[key]?.toString()

fun Properties.copyTo(outProperties: MutableMap<String, Any>) {
  for ((key, value) in this) {
    if (key is String) {
      if (key == "jxbrowser.license.key" || key.startsWith("dweb-")) {
        outProperties[key] = value
      }
    }
  }
}
