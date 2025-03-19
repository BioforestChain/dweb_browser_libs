@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustPosixTarget
import gobley.gradle.rust.targets.RustWindowsTarget
import gobley.gradle.uniffi.tasks.BuildBindingsTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import gobley.gradle.Variant

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
  project.description = "桌面端硬件信息模块"
  project.version = "1.2.0"
}

val isPublish =
  gradle.startParameter.taskNames.any { it.endsWith("publish") || it.endsWith("publishToMavenLocal") }

kotlin {
  jvm("desktop")
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  applyDefaultHierarchyTemplate {
    common {
      withJvm()
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.kotlinx.atomicfu)
      implementation(libs.squareup.okio)
      implementation(libs.kotlinx.datetime)
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
  jvmVariant = gobley.gradle.Variant.Release
  nativeVariant = gobley.gradle.Variant.Release

  builds.jvm {
    embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
  }
}

uniffi {
  generateFromUdl {
    namespace = "hardware_info"
    build =
      if (GobleyHost.Platform.MacOS.isCurrent) RustPosixTarget.MinGWX64 else {
        when (GobleyHost.Arch.Arm64.isCurrent) {
          true -> RustWindowsTarget.Arm64
          else -> RustWindowsTarget.X64
        }
      }
    variant = Variant.Release
    udlFile = layout.projectDirectory.file("uniffi/hardware_info.udl")
  }
}

var outputDirectoryPath = ""
tasks.withType<BuildBindingsTask> {
  doLast {
    outputDirectoryPath = outputDirectory.get().asFile.path
  }
}
tasks.named("compileKotlinDesktop") {
  doFirst {
    projectDir.resolve("src").deleteRecursively()
  }
  doLast {
    copyDirectoryToTarget(
      outputDirectoryPath,
      layout.projectDirectory.dir("src").asFile.path
    ) { source, target ->
      if (source.path.contains("commonMain") || source.path.contains("jvmMain")) {
        source.copyTo(target, true)
      }
      if (!source.path.contains("nativeInterop")) {
        source.delete()
      }
    }
  }
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
