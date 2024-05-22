plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
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
  sourceSets.commonTest.dependencies {
    kotlin("test")
  }
}

