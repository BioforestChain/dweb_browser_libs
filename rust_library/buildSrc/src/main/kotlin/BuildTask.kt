import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File

abstract class AndroidBuildTask : DefaultTask() {
  @get:Input
  abstract val exec: Property<ExecOperations>

  @get:Input
  abstract val ndkAbis: ListProperty<String>

  @get:Input
  abstract val projectRootDir: Property<String>

  private fun getArgs(target: String) = mutableListOf("cargo", "ndk").apply {
    addAll(
      listOf(
        "-t",
        target,
        "-o",
        "src/androidMain/jniLibs",
        "build",
        "--release",
        "--quiet",
      )
    )
  }

  @TaskAction
  fun run() {
    val exec = exec.get()

    run {
      ArchAndRustTargetMapping.abisToRustTargetMapping.forEach {
        exec.exec {
          environment("CARGO_BUILD_TARGET_DIR", File(projectRootDir.get()).resolve("target").path)
          workingDir = File(projectRootDir.get())
          commandLine = getArgs(it.value)
        }
      }
    }
  }
}

abstract class RustTargetBuildTask : DefaultTask() {
  @get:Input
  abstract val exec: Property<ExecOperations>

  @get:Input
  abstract val rustTargets: ListProperty<String>

  @get:Input
  abstract val projectRootDir: Property<String>

  @get:Input
  abstract val platform: Property<String>

  @get:Input
  abstract val copyTo: Property<(String, String) -> Unit>

  private fun getArgs(target: String) = mutableListOf("cargo").apply {
    addAll(
      listOf(
        "build",
        "--target",
        target,
        "--release",
        "--quiet",
      )
    )
  }

  @get:Input
  private val copyTasks = mutableListOf<CopyTaskItem>()

  private data class CopyTaskItem(val source: String, val target: String)

  fun runAllCopyTasks() {
    copyTasks.forEach {
      copyTo.get().invoke(it.source, it.target)
    }
  }

  @TaskAction
  fun run() {
    val exec = exec.get()
    val rootDir = File(projectRootDir.get())

    run {
      rustTargets.get().forEach {
        exec.exec {
          workingDir = rootDir
          environment("CARGO_BUILD_TARGET_DIR", rootDir.resolve("target").path)
          commandLine = getArgs(it)
        }

        when (platform.get()) {
          "iOS" -> {
            val filename = "${project.name.replace("lib_", "lib")}.a"
            copyTasks.add(
              CopyTaskItem(
                rootDir.resolve("target").resolve(it).resolve("release")
                  .resolve(filename).path,
                rootDir.resolve("src").resolve("libs").resolve(
                  ArchAndRustTargetMapping.iosRustTargetToArchMapping[it]!!
                ).resolve(filename).path
              )
            )
          }

          "macos" -> {
            val filename = "${project.name.replace("lib_", "lib")}.dylib"
            copyTasks.add(
              CopyTaskItem(
                rootDir.resolve("target").resolve(it).resolve("release")
                  .resolve(filename).path,
                rootDir.resolve("src").resolve("desktopMain").resolve("resources")
                  .resolve(ArchAndRustTargetMapping.macRustTargetToArchMapping[it]!!)
                  .resolve(filename).path
              )
            )
          }

          "win" -> {
            val filename = "${project.name.replace("lib_", "")}.dll"
            copyTasks.add(
              CopyTaskItem(
                rootDir.resolve("target").resolve(it).resolve("release")
                  .resolve(filename).path,
                rootDir.resolve("src").resolve("desktopMain").resolve("resources")
                  .resolve(ArchAndRustTargetMapping.winRustTargetToArchMapping[it]!!)
                  .resolve(filename).path
              )
            )
          }

          else -> {}
        }
      }
    }
  }
}