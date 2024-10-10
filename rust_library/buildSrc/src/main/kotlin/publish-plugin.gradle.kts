import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.signing

plugins {
  `maven-publish` // 发布插件
  signing // 签名插件，发布的时候需要对包进行签名
}

afterEvaluate {
  val properties = project.localProperties()
  publishing {
    publications {
      // 发布一个统一的 multiplatform 版本，整合所有支持的目标平台
      create<MavenPublication>("multiplatform") {
        from(components["kotlin"])  // 自动发布所有 Kotlin Multiplatform 支持的目标平台
        groupId = "io.github.BioforestChain"
        artifactId = project.name
        version = project.version.toString()
        println("开始签名=> ${project.description} version:${project.version} ${project.name}")
        pom {
          name = project.name
          description = project.description
          url =
            "https://github.com/BioforestChain/dweb_browser_libs/tree/main/rust_library/${project.name}"
        }
      }
    }
    repositories {
      // 远程 Maven 仓库
      maven {
        name = project.name
        // change URLs to point to your repos, e.g. http://my.org/repo
        val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        println("username: ${properties.getString("sonatypePassword")}")
        credentials {
          username = properties.getString("sonatypeUsername")
          password = properties.getString("sonatypePassword")
        }
      }
    }
  }

  signing {
    setRequired {
      gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
    }
    useInMemoryPgpKeys(
      properties.getString("signing.keyId"),
      properties.getString("signing.secretKeyRingFile"),
      properties.getString("signing.password"),
    )
    sign(publishing.publications["multiplatform"])
  }
}

