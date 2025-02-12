import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.assign


fun MavenPublication.configurePom(project: Project) {
  pom {
    name = project.name
    description = project.description
    url =
      "https://github.com/BioforestChain/dweb_browser_libs/tree/main/rust_library/${project.name}"
    inceptionYear.set("2025")

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
