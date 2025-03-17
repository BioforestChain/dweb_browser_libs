import org.gradle.kotlin.dsl.support.serviceOf

val execOperations = serviceOf<ExecOperations>()

tasks.register<RustTargetBuildTask>("build-ios") {
  exec.set(execOperations)
  projectRootDir.set(projectDir.path)
  rustTargets.set(ArchAndRustTargetMapping.iosRustTargetToArchMapping.keys)
  platform.set("iOS")
  copyTo.set { source, target ->
    File(source).copyTo(File(target), overwrite = true)
  }
  doLast {
    this@register.runAllCopyTasks()
  }
}

tasks.register<RustTargetBuildTask>("build-macos") {
  exec.set(execOperations)
  projectRootDir.set(projectDir.path)
  rustTargets.set(ArchAndRustTargetMapping.macRustTargetToArchMapping.keys)
  platform.set("macos")
  copyTo.set { source, target ->
    File(source).copyTo(File(target), overwrite = true)
  }
  doLast {
    this@register.runAllCopyTasks()
  }
}

tasks.register<RustTargetBuildTask>("build-win-x86_64") {
  exec.set(execOperations)
  projectRootDir.set(projectDir.path)
  rustTargets.set(ArchAndRustTargetMapping.winRustTargetToArchMapping.keys.filter {
    it.contains("x86") && it.contains(
      "msvc"
    )
  })
  platform.set("win")
  copyTo.set { source, target ->
    File(source).copyTo(File(target), overwrite = true)
  }
  doLast {
    this@register.runAllCopyTasks()
  }
}

tasks.register<RustTargetBuildTask>("build-win-arm64") {
  exec.set(execOperations)
  projectRootDir.set(projectDir.path)
  rustTargets.set(ArchAndRustTargetMapping.winRustTargetToArchMapping.keys.filter {
    it.contains("aarch64") && it.contains(
      "msvc"
    )
  })
  platform.set("win")
  copyTo.set { source, target ->
    File(source).copyTo(File(target), overwrite = true)
  }
  doLast {
    this@register.runAllCopyTasks()
  }
}

tasks.register<RustTargetBuildTask>("build-win") {
  exec.set(execOperations)
  projectRootDir.set(projectDir.path)
  rustTargets.set(ArchAndRustTargetMapping.winRustTargetToArchMapping.keys.filter { it.contains("gnu") })
  platform.set("win")
  copyTo.set { source, target ->
    File(source).copyTo(File(target), overwrite = true)
  }
  doLast {
    this@register.runAllCopyTasks()
  }
}

tasks.register("gen-bindings") {
  val task = tasks.findByName("prepareKotlinIdeaImport")
  if (task != null) {
    dependsOn(task)
  }
}

tasks.named("prepareKotlinIdeaImport") {
  doLast {
    if (!projectDir.resolve("build").resolve("generated").resolve("uniffi").exists()) {
      val osName = System.getProperty("os.name")
      execOperations.exec {
        commandLine =
          listOf(
            if (osName.startsWith("Mac")) rootDir.resolve("gradlew").path else rootDir.resolve("gradlew.bat").path,
            ":${project.name}:build"
          )
      }
    }
  }
}

tasks.register("cleanup-bindings") {
  doFirst {
    projectDir.resolve("src").deleteRecursively()
    projectDir.resolve("build").deleteRecursively()
  }
}

tasks.register("cleanup-targets") {
  doFirst {
    projectDir.resolve("target").deleteRecursively()
  }
}

tasks.register("cleanup-all") {
  dependsOn("cleanup-bindings")
  dependsOn("cleanup-targets")
}

tasks.withType<Copy> {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
}