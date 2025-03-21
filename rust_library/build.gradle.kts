plugins {
  //trick: for the same plugin versions in all sub-modules
  alias(libs.plugins.androidLibrary).apply(false)
  alias(libs.plugins.kotlinxMultiplatform).apply(false)
  `kotlin-dsl`
}

dependencies {
  implementation(libs.dev.gobley.rust)
  implementation(libs.dev.gobley.cargo)
  implementation(libs.dev.gobley.uniffi)
}

tasks.register("cleanup-root-target") {
  doFirst {
    rootDir.resolve("target").deleteRecursively()
  }
}

tasks.register("cleanup-targets") {
  subprojects.forEach { subproject ->
    val task = subproject.tasks.findByName("cleanup-targets")

    if (task != null) {
      dependsOn(task)
    }
  }

  dependsOn("cleanup-root-target")
}

tasks.register("cleanup-all") {
  subprojects.forEach { subproject ->
    val task = subproject.tasks.findByName("cleanup-all")

    if (task != null) {
      dependsOn(task)
    }
  }

  dependsOn("cleanup-root-target")
}

if (Platform.isWindows) {
  tasks.register("win-rust-resources-zip") {
    subprojects.forEach { subproject ->
      val projectWinResourcesDir =
        subproject.projectDir.resolve("src").resolve("desktopMain").resolve("resources")
      if (projectWinResourcesDir.exists()) {
        ArchAndRustTargetMapping.winRustTargetToArchMapping.values.forEach { arch ->
          val winArchResources = projectWinResourcesDir.resolve(arch)
          if (winArchResources.exists()) {
            winArchResources.copyRecursively(
              rootDir.resolve(".kotlin").resolve(arch).resolve(subproject.name.replace("lib_", ""))
                .resolve(arch), true
            )
          }
        }
      }
    }

    doLast {
      ArchAndRustTargetMapping.winRustTargetToArchMapping.values.forEach { arch ->
        val sourceDir = rootDir.resolve(".kotlin").resolve(arch)
        if (sourceDir.exists()) {
          createZipFile(sourceDir.path, rootDir.resolve(".kotlin").resolve("$arch.zip").path)
        }
      }
    }
  }
}

tasks.register("win-rust-resources-unzip") {
  ArchAndRustTargetMapping.winRustTargetToArchMapping.values.forEach { arch ->
    val zipFile = rootDir.resolve(".kotlin").resolve("$arch.zip")
    if (zipFile.exists()) {
      val targetDir = rootDir.resolve(".kotlin").resolve(arch)
      unzip(zipFile, targetDir)

      for (file in targetDir.listFiles() ?: emptyArray()) {
        if (file.isDirectory) {
          file.copyRecursively(
            rootDir.resolve(file.name).resolve("src").resolve("desktopMain")
              .resolve("resources"), true
          )
        }
      }

      val unzipDir = rootDir.resolve(".kotlin").resolve(arch)
      if (unzipDir.exists()) {
        unzipDir.deleteRecursively()
      }
    }
  }
}
