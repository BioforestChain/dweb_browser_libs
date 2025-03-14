@file:OptIn(ExperimentalKotlinGradlePluginApi::class, InternalGobleyGradleApi::class)

import gobley.gradle.GobleyHost
import gobley.gradle.InternalGobleyGradleApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  `publish-plugin`
  `build-libs-plugin`
  alias(libs.plugins.devGobleyRust)
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
