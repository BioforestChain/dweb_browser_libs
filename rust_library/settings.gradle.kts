enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
  repositories {
    google {
      mavenContent {
        includeGroupByRegex(".*google.*")
        includeGroupByRegex(".*android.*")
      }
    }
    gradlePluginPortal()
    mavenCentral()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    google {
      mavenContent {
        includeGroupByRegex(".*google.*")
        includeGroupByRegex(".*android.*")
      }
    }
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.9.0")
}

//includeBuild("./build-logic")

rootProject.name = "rust-library"
//include(":ziplib")
//include(":reverse_proxy")
//include(":multipart")
//include(":biometrics")
rootDir.listFiles { file -> file.isDirectory }
  ?.forEach { dir ->
    if (dir.name == "resvg_render" && File(dir, "build.gradle.kts").exists()) {
      include(dir.name)
      project(":${dir.name}").apply {
        name = "lib_${dir.name}"
        projectDir = file(dir)
        buildFileName = "build.gradle.kts"
      }
    }
  }