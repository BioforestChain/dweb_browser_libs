@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.Variant
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustPosixTarget
import gobley.gradle.rust.targets.RustWindowsTarget
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
    embedRustLibrary = if (GobleyHost.Platform.MacOS.isCurrent) {
      (rustTarget == RustPosixTarget.MacOSArm64 || rustTarget == RustPosixTarget.MacOSX64)
    } else if (GobleyHost.Platform.Windows.isCurrent) {
      (rustTarget == GobleyHost.current.rustTarget)
    } else {
      false
    }
  }
}

uniffi {
  generateFromUdl {
    namespace = "keychainstore"
    build = if (GobleyHost.Platform.MacOS.isCurrent) {
      when (GobleyHost.Arch.Arm64.isCurrent) {
        true -> RustPosixTarget.MacOSArm64
        else -> RustPosixTarget.MacOSX64
      }
    } else {
      when (GobleyHost.Arch.Arm64.isCurrent) {
        true -> RustWindowsTarget.Arm64
        else -> RustWindowsTarget.X64
      }
    }
    variant = Variant.Release
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