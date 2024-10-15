plugins {
  `maven-publish` // 发布插件
  signing // 签名插件，发布的时候需要对包进行签名
}

val localProperties = project.localProperties()

ext["signing.keyId"] = localProperties.getString("signing.keyId")
ext["signing.secretKeyRingFile"] =
  rootDir.resolve(localProperties.getString("signing.secretKeyRingFile")).absolutePath
ext["signing.password"] = localProperties.getString("signing.password")

afterEvaluate {
  publishing {
    publications {
      // 发布一个统一的 multiplatform 版本，整合所有支持的目标平台
      create<MavenPublication>("multiplatform") {
        from(components["kotlin"])  // 自动发布所有 Kotlin Multiplatform 支持的目标平台
        groupId = "io.github.dweb-channel"
        group = "io.github.dweb-channel"
        artifactId = project.name
        version = project.version.toString()
        pom {
          name = project.name
          description = project.description
          url =
            "https://github.com/BioforestChain/dweb_browser_libs/tree/main/rust_library/${project.name}"
          inceptionYear.set("2024")

          licenses {
            license {
              name.set("MIT")
              url.set("https://opensource.org/licenses/MIT")
            }
          }
          // Specify developers information
          developers {
            developer {
              id.set("BioforestChain")
              name.set("dweb")
              email.set("developer@bfchain.org")
            }
          }
          // Specify SCM information
          scm {
            url.set("https://github.com/BioforestChain/dweb_browser_libs")
          }
        }
      }
    }
    repositories {
      // 远程 Maven 仓库
      maven {
        this.artifactUrls
//        name = project.name
        // change URLs to point to your repos, e.g. http://my.org/repo
//        val releasesRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
        val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        credentials {
          username = localProperties.getString("ossrhUsername")
          password = localProperties.getString("ossrhPassword")
        }
      }
    }
  }

  signing {
    setRequired {
      gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }

    sign(publishing.publications["multiplatform"])
  }

  tasks.withType<Sign> {
    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
  }
}
