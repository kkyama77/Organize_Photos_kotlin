pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        val kotlinVersion = extra["kotlin.version"] as String? ?: "1.9.23"
        val composeVersion = extra["compose.version"] as String? ?: "1.6.11"
        val agpVersion = extra["agp.version"] as String? ?: "8.3.2"

        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
        id("com.android.application") version agpVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "organize_photos_kotlin"
include(":composeApp")
