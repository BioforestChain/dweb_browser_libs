@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.Variant
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustAndroidTarget
import gobley.gradle.rust.targets.RustPosixTarget
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  `publish-plugin`
  `build-libs-plugin`
  alias(libs.plugins.devGobleyRust)
  alias(libs.plugins.devGobleyCargo)
  alias(libs.plugins.devGobleyUniffi)
  alias(libs.plugins.kotlin.atomicfu)
}
plugins.withId("publish-plugin") {
  project.description = "跨平台压缩解压库"
  project.version = "1.2.1"
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
    compilations.all {
      (this as? KotlinJvmCompile)?.compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
      }
    }
  }

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
        baseName = "mix_compression"
        isStatic = true
      }
    }
  }

  applyDefaultHierarchyTemplate()

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

    androidMain.dependencies {
      api(libs.java.jna.map {
        project.dependencies.create(it, closureOf<ExternalModuleDependency> {
          artifact {
            type = "aar"
          }
        })
      })
    }

    val desktopMain = sourceSets.getByName("desktopMain")
    desktopMain.dependencies {
      api(libs.java.jna)
    }
  }
}

android {
  namespace = "org.dweb_browser.mix_compression"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
    ndk {
      abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
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
    namespace = "mix_compression"
    build = RustAndroidTarget.Arm64
    variant = Variant.Release
    udlFile = layout.projectDirectory.file("uniffi/mix_compression.udl")
  }
//  formatCode = true
}

tasks.named("compileKotlinDesktop") {
  doFirst {
    if (!project.isPublish) {
      projectDir.resolve("src").deleteRecursively()
    } else {
      projectDir.resolve("build").resolve("generated").resolve("uniffi").listFiles().forEach {
        if (it.path.contains("Main")) {
          it.deleteRecursively()
        }
      }
    }
  }
  doLast {
    copyDirectoryToTarget(
      layout.projectDirectory.dir("build").dir("generated").dir("uniffi").asFile.path,
      layout.projectDirectory.dir("src").asFile.path
    ) { source, target ->
      source.copyTo(target, true)
      if (!source.path.contains("nativeInterop")) {
        source.delete()
      }
    }
    copyFileToTarget(
      layout.projectDirectory.file("mix_compression.def").asFile.path,
      layout.projectDirectory.dir("src").dir("nativeInterop").dir("cinterop").asFile.path
    ) { source, target ->
      source.copyTo(target, true)
    }
  }
}

val execOperations = serviceOf<ExecOperations>()
tasks.register<AndroidBuildTask>("build-android") {
  exec.set(execOperations)
  ndkAbis.set(android.defaultConfig.ndk.abiFilters.toList())
  projectRootDir.set(projectDir.path)
}

tasks.register("macos-rust-process") {
  dependsOn("build-ios")
  finalizedBy("rust-resources-copy")
}

tasks.register("win-gnu-cargo-build") {
  dependsOn("build-win")
  finalizedBy("rust-resources-copy")
}

tasks.named("gen-bindings") {
  if (GobleyHost.Platform.MacOS.isCurrent) {
    finalizedBy("macos-rust-process")
  } else {
    finalizedBy("rust-resources-copy")
  }
}

project.afterEvaluate {
  tasks.named("buildBindings") {
    doLast {
      if (project.isPublish) {
        projectDir.resolve("build").resolve("generated").resolve("uniffi").listFiles().forEach {
          if (it.path.contains("Main")) {
            it.deleteRecursively()
          }
        }
      }
    }
  }
}