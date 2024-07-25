plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
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
      baseName = "mix_compression"
    }
    val main by it.compilations.getting
    main.cinterops.create("mix_compression") {
      includeDirs(project.file("src/nativeInterop/cinterop/headers/mix_compression"), project.file("src/libs/${it.targetName}"))
    }
  }

  @Suppress("OPT_IN_USAGE")
  applyDefaultHierarchyTemplate()

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

  sourceSets.androidMain.dependencies {
    api(libs.java.jna.map {
      project.dependencies.create(it, closureOf<ExternalModuleDependency> {
        artifact {
          type = "aar"
        }
      })
    })
  }
}

android {
  namespace = "org.dweb_browser.mix_compression"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
}
