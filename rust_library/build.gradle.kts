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

tasks.register("cleanup-root-target") {
  doFirst {
    rootDir.resolve("target").deleteRecursively()
  }
}

tasks.register("cleanup-targets") {
  subprojects.forEach { subproject ->
    val task = subproject.tasks.findByName("cleanup-targets")

    if (task != null) {
      dependsOn(task)
    }
  }

  dependsOn("cleanup-root-target")
}

tasks.register("cleanup-all") {
  subprojects.forEach { subproject ->
    val task = subproject.tasks.findByName("cleanup-all")

    if (task != null) {
      dependsOn(task)
    }
  }

  dependsOn("cleanup-root-target")
}