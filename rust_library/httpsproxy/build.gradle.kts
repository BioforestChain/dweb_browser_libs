plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
}

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
      }
    }
  }

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "httpsproxy"
    }
    val main by it.compilations.getting
    main.cinterops.create("httpsproxy") {
      includeDirs(project.file("src/nativeInterop/cinterop/headers/httpsproxy"), project.file("src/libs/${it.targetName}"))
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }
  sourceSets.commonMain.dependencies {
    api(libs.kotlinx.atomicfu)
    implementation(libs.squareup.okio)
    implementation(libs.kotlinx.datetime)
  }
  sourceSets.commonTest.dependencies {
    kotlin("test")
  }
  sourceSets.androidMain.dependencies {
    implementation(libs.java.jna)
  }
}

android {
  namespace = "org.dweb_browser.httpsproxy"
  compileSdk = 34
  defaultConfig {
    minSdk = 29
    consumerProguardFiles("consumer-rules.pro")
  }
}
