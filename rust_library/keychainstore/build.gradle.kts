@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.Variant
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.uniffi.tasks.BuildBindingsTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  `publish-plugin`
  `build-libs-plugin`
  alias(libs.plugins.devGobleyRust)
  alias(libs.plugins.devGobleyCargo)
  alias(libs.plugins.devGobleyUniffi)
  alias(libs.plugins.kotlin.atomicfu)
}
plugins.withId("publish-plugin") {
  project.description = "desktop/ios平台密钥存储模块"
  project.version = "1.2.0"
}

val isPublish =
  gradle.startParameter.taskNames.any { it.endsWith("publish") || it.endsWith("publishToMavenLocal") }


kotlin {
  jvm("desktop")

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  if (GobleyHost.Platform.MacOS.isCurrent) {
    listOf(
      iosX64(),
      iosArm64(),
      iosSimulatorArm64()
    ).forEach {
      it.binaries.framework {
        baseName = "keychainstore"
        isStatic = true
      }
    }
  }

  applyDefaultHierarchyTemplate {
    common {
      withJvm()
      withIos()
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.kotlinx.atomicfu)
      implementation(libs.squareup.okio)
      implementation(libs.kotlinx.datetime)
      implementation(libs.kotlinx.coroutines.core)
    }

    commonTest.dependencies {
      kotlin("test")
    }

    val desktopMain = sourceSets.getByName("desktopMain")
    desktopMain.dependencies {
      api(libs.java.jna)
    }
  }
}

cargo {
  packageDirectory = layout.projectDirectory.dir("uniffi")
  jvmVariant = Variant.Release
  nativeVariant = Variant.Release

  builds.jvm {
    embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
  }
}

uniffi {
  generateFromUdl {
    namespace = "keychainstore"
//    build = RustJvmTarget
//    variant = Variant.Debug
    udlFile = layout.projectDirectory.file("uniffi/keychainstore.udl")
  }
//  formatCode = true
}

var outputDirectoryPath = ""
tasks.withType<BuildBindingsTask> {
  doLast {
    outputDirectoryPath = outputDirectory.get().asFile.path
  }
}

tasks.named("prepareKotlinIdeaImport") {
  doLast {
    copyDirectoryToTarget(
      outputDirectoryPath,
      layout.projectDirectory.dir("src").asFile.path
    ) { source, target ->
      if (!source.path.contains("androidMain")) {
        source.copyTo(target, true)
      }
      if (!source.path.contains("nativeInterop")) {
        source.delete()
      }
    }
    copyFileToTarget(
      layout.projectDirectory.file("keychainstore.def").asFile.path,
      layout.projectDirectory.dir("src").dir("nativeInterop").dir("cinterop").asFile.path
    ) { source, target ->
      source.copyTo(target, true)
    }
  }
}

tasks.register("macos-cargo-build") {
  dependsOn("build-ios")
  dependsOn("build-macos")
}

tasks.register("win-cargo-build") {
  if (GobleyHost.Arch.Arm64.isCurrent) {
    dependsOn("build-win-arm64")
  } else {
    dependsOn("build-win-x86_64")
  }
}

tasks.register("win-gnu-cargo-build") {
  dependsOn("build-win")
}

project.afterEvaluate {
  tasks.named("buildBindings") {
    doLast {
      if (isPublish) {
        projectDir.resolve("build").resolve("generated").resolve("uniffi").listFiles().forEach {
          if (it.path.contains("Main")) {
            it.deleteRecursively()
          }
        }
      }
    }
  }
}