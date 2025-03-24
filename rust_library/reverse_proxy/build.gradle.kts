@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import gobley.gradle.Variant
import gobley.gradle.rust.targets.RustAndroidTarget
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
  project.description = "反向代理网络库"
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
        baseName = "reverse_proxy"
        isStatic = true
      }
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      api(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.squareup.okio)
      implementation(libs.kotlinx.datetime)
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
  }
}

android {
  namespace = "org.dweb_browser.reverse_proxy"
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
}

uniffi {
  generateFromUdl {
    namespace = "reverse_proxy"
    build = RustAndroidTarget.Arm64
    variant = Variant.Release
    udlFile = layout.projectDirectory.file("uniffi/reverse_proxy.udl")
  }
//  formatCode = true
}

tasks.named("prepareKotlinIdeaImport") {
  doLast {
    copyDirectoryToTarget(
      layout.projectDirectory.dir("build").dir("generated").dir("uniffi").asFile.path,
      layout.projectDirectory.dir("src").asFile.path
    ) { source, target ->
      if (!source.path.contains("jvmMain")) {
        source.copyTo(target, true)
      }
      if (!source.path.contains("nativeInterop")) {
        source.delete()
      }
    }
    copyFileToTarget(
      layout.projectDirectory.file("reverse_proxy.def").asFile.path,
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
  dependsOn("prepareKotlinIdeaImport")
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