plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  `publish-plugin`
}

plugins.withId("publish-plugin") {
  project.description = "反向代理网络库"
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

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "reverse_proxy"
    }
    val main by it.compilations.getting
    main.cinterops.create("reverse_proxy") {
      includeDirs(
        project.file("src/nativeInterop/cinterop/headers/reverse_proxy"),
        project.file("src/libs/${it.targetName}")
      )
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }
  sourceSets.commonMain.dependencies {
    api(libs.kotlinx.atomicfu)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.squareup.okio)
    implementation(libs.kotlinx.datetime)
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
}

android {
  namespace = "org.dweb_browser.reverse_proxy"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
}