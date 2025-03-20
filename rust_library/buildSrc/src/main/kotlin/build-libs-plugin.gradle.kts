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

tasks.register("rust-resources-copy") {
  doLast {
    val parentDir = projectDir.resolve("build").resolve("intermediates").resolve("rust")
    if (parentDir.exists()) {
      parentDir.listFiles()!!.forEach { rustTarget ->
        if (rustTarget.name in ArchAndRustTargetMapping.androidRustTargetToArchMapping.keys) {
          val arch = ArchAndRustTargetMapping.androidRustTargetToArchMapping[rustTarget.name]!!
          val source = parentDir.resolve(rustTarget.name).resolve("release").resolve(arch)
          val target =
            projectDir.resolve("src").resolve("androidMain").resolve("jniLibs").resolve(arch)
          source.copyRecursively(target, true)
        } else if (rustTarget.name in ArchAndRustTargetMapping.macRustTargetToArchMapping.keys) {
          val arch = ArchAndRustTargetMapping.macRustTargetToArchMapping[rustTarget.name]!!
          val source = parentDir.resolve(rustTarget.name).resolve("release").resolve(arch)
          val target =
            projectDir.resolve("src").resolve("desktopMain").resolve("resources").resolve(arch)
          source.copyRecursively(target, true)
        } else if (rustTarget.name in ArchAndRustTargetMapping.winRustTargetToArchMapping.keys) {
          val arch = ArchAndRustTargetMapping.winRustTargetToArchMapping[rustTarget.name]!!
          val source = parentDir.resolve(rustTarget.name).resolve("release").resolve(arch)
          val target =
            projectDir.resolve("src").resolve("desktopMain").resolve("resources").resolve(arch)
          source.copyRecursively(target, true)
        }
      }
    }
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
  doFirst {
    projectDir.resolve("src").deleteRecursively()
  }
  doLast {
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

tasks.register("cleanup-bindings") {
  doFirst {
    projectDir.resolve("src").deleteRecursively()
//    projectDir.resolve("build").deleteRecursively()
    projectDir.resolve("build").listFiles().forEach { file ->
      if (file.name != "bindgen-install") {
        file.deleteRecursively()
      }
    }
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