import java.io.File

object ArchAndRustTargetMapping {
  val iosRustTargetToArchMapping = mutableMapOf(
    "aarch64-apple-ios" to "iosArm64",
    "x86_64-apple-ios" to "iosX64",
    "aarch64-apple-ios-sim" to "iosSimulatorArm64"
  )
  val abisToRustTargetMapping =
    mutableMapOf(
      "armeabi-v7a" to "armv7-linux-androideabi",
      "arm64-v8a" to "aarch64-linux-android",
      "x86" to "i686-linux-android",
      "x86_64" to "x86_64-linux-android"
    )

  val winRustTargetToArchMapping = mutableMapOf(
    "aarch64-pc-windows-msvc" to "win32-aarch64",
    "x86_64-pc-windows-msvc" to "win32-x86-64",
    "aarch64-pc-windows-gnullvm" to "win32-aarch64",
    "x86_64-pc-windows-gnu" to "win32-x86-64"
  )

  val macRustTargetToArchMapping = mutableMapOf(
    "aarch64-apple-darwin" to "darwin-aarch64", "x86_64-apple-darwin" to "darwin-x86-64"
  )
}

fun copyFileToTarget(
  sourcePath: String,
  targetPath: String,
  hardLinkOrCopyFile: (source: File, target: File) -> Unit
) {
  val source = File(sourcePath)

  if (source.isDirectory) {
    val currentTargetPath = File(targetPath).resolve(
      when (source.name) {
        "jvmMain" -> "desktopMain"
        "nativeMain" -> "iosMain"
        else -> source.name
      }
    ).path
    source.listFiles().forEach { file ->
      copyFileToTarget(file.path, currentTargetPath, hardLinkOrCopyFile)
    }
  } else {
    if (source.name == "uniffi.toml" || source.name == "dummy.def") return
    hardLinkOrCopyFile(
      source,
      File(targetPath).resolve(
        source.name.replace(".jvm.", ".desktop.").replace(".native.", ".ios.")
      )
    )
  }
}

fun copyDirectoryToTarget(
  sourcePath: String,
  targetPath: String,
  hardLinkOrCopyFile: (source: File, target: File) -> Unit
) {
  val source = File(sourcePath)

  if (source.isDirectory) {
    source.listFiles().forEach { file ->
      copyFileToTarget(file.path, targetPath, hardLinkOrCopyFile)
    }
  }
}