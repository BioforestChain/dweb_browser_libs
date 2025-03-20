import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

  val androidRustTargetToArchMapping = mutableMapOf(
    "armv7-linux-androideabi" to "armeabi-v7a",
    "aarch64-linux-android" to "arm64-v8a",
    "i686-linux-android" to "x86",
    "x86_64-linux-android" to "x86_64"
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

fun createZipFile(sourceDirPath: String, outputZipFilePath: String) {
  val sourceDir = File(sourceDirPath)

  if (!sourceDir.exists()) {
    println("win平台rust静态链接库不存在")
    return
  }

  FileOutputStream(outputZipFilePath).use { fos ->
    ZipOutputStream(fos).use { zos ->
      addFilesToZip(sourceDir, sourceDir, zos)
    }
  }
}

private fun addFilesToZip(rootDir: File, currentDir: File, zos: ZipOutputStream) {
  for (file in currentDir.listFiles() ?: emptyArray()) {
    if (file.isDirectory) {
      // 如果是目录，递归处理
      addFilesToZip(rootDir, file, zos)
    } else {
      // 如果是文件，添加到 ZIP
      FileInputStream(file).use { fis ->
        val entryPath = rootDir.toPath().relativize(file.toPath()).toString()
        zos.putNextEntry(ZipEntry(entryPath))
        fis.copyTo(zos)
        zos.closeEntry()
      }
    }
  }
}

fun unzipFile(zipFilePath: String, outputDirPath: String) {
  val zipFile = File(zipFilePath)
  if (!zipFile.exists() || !zipFile.isFile) {
    println("ZIP file does not exist or is not a file: $zipFilePath")
    return
  }

  val outputDir = File(outputDirPath)
  if (!outputDir.exists()) {
    outputDir.mkdirs() // 如果目标目录不存在，则创建
  }

  // 打开 ZIP 文件并逐条解压
  ZipInputStream(FileInputStream(zipFile)).use { zis ->
    var entry: ZipEntry?
    while (zis.nextEntry.also { entry = it } != null) {
      val entryName = entry!!.name
      val outputFile = File(outputDir, entryName)

      if (entry!!.isDirectory) {
        // 如果是目录，创建目录
        outputFile.mkdirs()
      } else {
        // 如果是文件，创建父目录并写入文件内容
        outputFile.parentFile.mkdirs()
        FileOutputStream(outputFile).use { fos ->
          zis.copyTo(fos)
        }
      }

      // 关闭当前条目
      zis.closeEntry()
    }
  }
}