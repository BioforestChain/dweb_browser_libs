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
  project.description = "桌面端生物识别模块"
  project.version = "1.2.0"
}

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
    namespace = "biometrics"
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
    udlFile = layout.projectDirectory.file("uniffi/biometrics.udl")
  }
}

tasks.named("compileKotlinDesktop") {
  doFirst {
    projectDir.resolve("src").deleteRecursively()
  }
  doLast {
    copyDirectoryToTarget(
      layout.projectDirectory.dir("build").dir("generated").dir("uniffi").asFile.path,
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

tasks.register("macos-rust-process") {
  finalizedBy("rust-resources-copy")
}

tasks.register("win-gnu-cargo-build") {
  dependsOn("build-win")
  finalizedBy("rust-resources-copy")
}

tasks.named("gen-bindings") {
  finalizedBy("rust-resources-copy")
}