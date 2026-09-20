pluginManagement {
    repositories {
        maven {
            name = "EngineHub"
            url = uri("https://repo.enginehub.org/libs-release/")
        }
        maven {
            name = "EngineHub Central Mirror"
            url = uri("https://repo.enginehub.org/internal/maven-central-proxy/")
        }
        maven {
            name = "EngineHub Plugin Portal Mirror"
            url = uri("https://repo.enginehub.org/internal/plugin-portal-proxy/")
        }
    }
}
plugins {
    id("org.enginehub.crankcase.repo-reconfiguration") version "0.1.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "EngineHub"
            url = uri("https://repo.enginehub.org/libs-release/")
        }
    }
}

rootProject.name = "EngineHubBot"
