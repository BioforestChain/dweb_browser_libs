plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  `publish-plugin`
}
plugins.withId("publish-plugin") {
  project.description = "跨平台图片渲染库"
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
      baseName = "resvg_render"
    }
    val main by it.compilations.getting
    main.cinterops.create("resvg_render") {
      includeDirs(
        project.file("src/nativeInterop/cinterop/headers/resvg_render"),
        project.file("src/libs/${it.targetName}")
      )
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
  namespace = "org.dweb_browser.resvg_render"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
}
