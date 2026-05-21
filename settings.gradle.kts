pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TomTom SDK Maven Repository (Dinonaktifkan sementara untuk menghindari error 401)
        // Untuk mengaktifkannya, buka komentar di bawah ini dan pastikan Anda sudah menambahkan:
        // repositoryUsername=email_anda dan repositoryIdentityToken=token_anda di gradle.properties
        /*
        maven {
            url = uri("https://repositories.tomtom.com/artifactory/maven")
            credentials {
                username = providers.gradleProperty("repositoryUsername").orNull
                password = providers.gradleProperty("repositoryIdentityToken").orNull
            }
        }
        */
    }
}

rootProject.name = "NaikApa"
include(":app")
 