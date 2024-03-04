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

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "multipart"
    }
    val main by it.compilations.getting
    main.cinterops.create("multipart") {
      includeDirs(project.file("src/nativeInterop/cinterop/headers/multipart"), project.file("src/libs/${it.targetName}"))
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
    implementation(libs.kotlinx.coroutines.core)
  }
  sourceSets.commonTest.dependencies {
    kotlin("test")
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

  jvm("desktop")
  val desktopMain = sourceSets.getByName("desktopMain")
}

android {
  namespace = "org.dweb_browser.multipart"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
}
