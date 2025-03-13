plugins {
  //trick: for the same plugin versions in all sub-modules
  alias(libs.plugins.androidLibrary).apply(false)
  alias(libs.plugins.kotlinxMultiplatform).apply(false)
  `kotlin-dsl`
}

dependencies {
  implementation(libs.dev.gobley.rust)
  implementation(libs.dev.gobley.cargo)
  implementation(libs.dev.gobley.uniffi)
}
