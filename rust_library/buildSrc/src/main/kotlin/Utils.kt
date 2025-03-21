import java.io.BufferedOutputStream
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
      // 强制添加目录条目
      val entryPath = rootDir.toPath()
        .relativize(file.toPath())
        .toString().replace(File.separator, "/") + "/" // 追加斜杠
      zos.putNextEntry(ZipEntry(entryPath))
      zos.closeEntry()
      // 如果是目录，递归处理
      addFilesToZip(rootDir, file, zos)
    } else {
      // 统一文件路径分隔符为Unix格式
      val entryPath = rootDir.toPath()
        .relativize(file.toPath())
        .toString().replace(File.separator, "/")
      // 如果是文件，添加到 ZIP
      FileInputStream(file).use { fis ->
        zos.putNextEntry(ZipEntry(entryPath))
        fis.copyTo(zos)
        zos.closeEntry()
      }
    }
  }
}

// see: https://github.com/DaemonicLabs/Voodoo/blob/8e4a0643edf10d39335ea084d00d036606901fd4/util/src/main/kotlin/voodoo/util/UnzipUtility.kt#L34
// 添加跨平台处理解压处理
fun unzip(zipFile: File, destDir: File) {
  require(zipFile.exists()) { "$zipFile does not exist" }
  require(zipFile.isFile) { "$zipFile not not a file" }
  val destDir = destDir.absoluteFile
  if (!destDir.exists()) {
    destDir.mkdir()
  }

  val zipIn = ZipInputStream(FileInputStream(zipFile))
  var entry: ZipEntry? = zipIn.nextEntry
  // iterates over entries in the zip file
  while (entry != null) {
    // 统一转换为平台路径分隔符
//    val entryName = entry.name.replace("/", File.separator)
    val entryName = entry.name.replace("/", File.separator)
    val filePath = destDir.resolve(entryName).path
    if (!entry.isDirectory) {
      // if the entry is a file, extracts it
      File(filePath).parentFile.mkdirs()
      extractFile(zipIn, filePath)
    } else {
      // if the entry is a directory, make the directory
      val dir = File(filePath)
      dir.mkdir()
    }
    zipIn.closeEntry()
    entry = zipIn.nextEntry
  }
  zipIn.close()
}

private fun extractFile(zipIn: ZipInputStream, filePath: String) {
  val bos = BufferedOutputStream(FileOutputStream(filePath))
  val bytesIn = ByteArray(4096)
  var read: Int
  while (true) {
    read = zipIn.read(bytesIn)
    if (read < 0) break
    bos.write(bytesIn, 0, read)
  }
  bos.close()
}