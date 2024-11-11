pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven { url = uri("https://clojars.org/repo") }
        maven { url = uri("https://maven.google.com") }
    }
}
rootProject.name = "SimpMusic"
include ("app")
include(":kotlinYtmusicScraper")
include(":doubletapplayerview")
