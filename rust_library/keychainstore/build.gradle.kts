plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  `publish-plugin`
}
plugins.withId("publish-plugin") {
  project.description = "跨平台密钥存储模块"
  project.version = "1.0.0"
}

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
      }
    }
  }

  jvm("desktop")

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "keychainstore"
    }
    val main by it.compilations.getting
    main.cinterops.create("keychainstore") {
      includeDirs(project.file("src/nativeInterop/cinterop/headers/keychainstore"), project.file("src/libs/${it.targetName}"))
    }
  }

  @Suppress("OPT_IN_USAGE")
  applyDefaultHierarchyTemplate {
    common {
      group("jvm") {
        withJvm()
      }
      withIos()
    }
  }

  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }
  sourceSets.commonMain.dependencies {
    api(libs.kotlinx.atomicfu)
    implementation(libs.squareup.okio)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
  }
  sourceSets.commonTest.dependencies {
    kotlin("test")
  }

  jvm("desktop")
  val desktopMain = sourceSets.getByName("desktopMain")
  desktopMain.dependencies {
    api(libs.java.jna)
  }
}

android {
  namespace = "org.dweb_browser.keychainstore"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
}
