plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  `publish-plugin`
}
plugins.withId("publish-plugin") {
  project.description = "桌面端硬件信息模块"
  project.version = "1.0.0"
}
kotlin {
  jvm("desktop")
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
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
  val desktopMain = sourceSets.getByName("desktopMain")
  desktopMain.dependencies {
    api(libs.java.jna)
  }
  sourceSets.commonTest.dependencies {
    kotlin("test")
  }
}

