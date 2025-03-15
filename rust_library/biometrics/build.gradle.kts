@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustPosixTarget
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
  jvmVariant = gobley.gradle.Variant.Release
  nativeVariant = gobley.gradle.Variant.Release

  builds.jvm {
    embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
  }
}

uniffi {
  generateFromUdl {
    namespace = "biometrics"
    build = RustPosixTarget.MacOSArm64
    udlFile = layout.projectDirectory.file("uniffi/biometrics.udl")
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

tasks.register("macos-cargo-build") {
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
