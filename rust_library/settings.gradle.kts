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

rootProject.name = "rust-library"
//include(":ziplib")
//include(":reverse_proxy")
//include(":multipart")
//include(":biometrics")
rootDir.listFiles { file -> file.isDirectory }
  ?.forEach { dir ->
    if (File(dir, "build.gradle.kts").exists()) {
      include(dir.name)
      project(":${dir.name}").apply {
        name = "lib_${dir.name}"
        projectDir = file(dir)
        buildFileName = "build.gradle.kts"
      }
    }
  }