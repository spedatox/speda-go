/*
 * SPEDA GO — the native Android client for SPEDA Mark VI.
 *
 * A self-contained Gradle build. It was split out of the speda-mark6 monorepo
 * (packages/heartbreaker-android) with its history intact and is inert to the
 * backend's GitOps deploy: the server never runs Gradle.
 */
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "speda-go"

include(":app")
include(":designsystem")
