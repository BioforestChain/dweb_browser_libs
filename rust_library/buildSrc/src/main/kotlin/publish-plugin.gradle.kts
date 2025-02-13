import java.util.Locale

plugins {
  `maven-publish` // 发布插件
  signing // 签名插件，发布的时候需要对包进行签名
}

val localProperties = project.localProperties()

/// 将签名信息写入properties中
ext["signing.keyId"] = localProperties.getString("signing.keyId")
ext["signing.secretKeyRingFile"] =
  rootDir.resolve(localProperties.getString("signing.secretKeyRingFile")).absolutePath
ext["signing.password"] = localProperties.getString("signing.password")

/// 生成javadoc，否则desktop目标无法同步到Central
val javadocJar = tasks.register("javadocJar", Jar::class.java) {
  archiveClassifier.set("javadoc")
}

afterEvaluate {
  val target = project.findProperty("Target").toString()

  /// 如果 target 为 github，则推送到 Github Packages
  if (target == "github") {
    publishing {
      publications {
        withType<MavenPublication> {
          group = "io.github.dweb-channel"
          groupId = "io.github.dweb-channel"
          version = project.version.toString()
          if (name == "kotlinMultiplatform") {
            artifactId = project.name
          } else {
            artifactId = project.name + "-${name.lowercase(Locale.getDefault())}"
          }
          artifact(javadocJar)
          configurePom(project)
        }
      }

      repositories {
        maven {
          setUrl("https://maven.pkg.github.com/BioforestChain/dweb_browser_libs")
          credentials {
            username = localProperties.getString("githubPackagesUsername")
            password = localProperties.getString("githubPackagesPassword")
          }
        }
      }
    }
    /// 默认推送到 Maven
  } else {
    publishing {
      publications {
        withType<MavenPublication> {
          group = "io.github.dweb-channel"
          groupId = "io.github.dweb-channel"
          version = project.version.toString()
          if (name == "kotlinMultiplatform") {
            artifactId = project.name
          } else {
            artifactId = project.name + "-${name.lowercase(Locale.getDefault())}"
          }
          artifact(javadocJar)
          configurePom(project)
        }
      }

      repositories {
        // 远程 Maven 仓库
        maven {
          val releasesRepoUrl =
            uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
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

      /// 使用命令行签名的方式
      useGpgCmd()
      sign(publishing.publications)
    }

    tasks.withType<Sign> {
      onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
    }

    /// 发布任务必须在签名任务之后
    tasks.withType<AbstractPublishToMaven>().configureEach {
      val signingTasks = tasks.withType<Sign>()
      mustRunAfter(signingTasks)
    }
  }
}
