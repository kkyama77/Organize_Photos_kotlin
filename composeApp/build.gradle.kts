import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.animation)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.0")
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
                implementation("com.drewnoakes:metadata-extractor:2.18.0")
                implementation("io.coil-kt:coil-compose:2.5.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("com.drewnoakes:metadata-extractor:2.18.0")
            }
        }
        val windowsMain by getting {
            dependsOn(desktopMain)
            dependencies {
                implementation("net.java.dev.jna:jna:5.14.0")
                implementation("net.java.dev.jna:jna-platform:5.14.0")
            }
        }
    }
}

android {
    namespace = "com.organize.photos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.organize.photos"
        minSdk = 26
        targetSdk = 34
        versionCode = project.property("app.version.code").toString().toInt()
        versionName = project.property("app.version.name").toString()
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.organize.photos.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OrganizePhotos"
            packageVersion = project.property("app.version.name").toString()
            description = "Photo organizer with EXIF metadata search"
            vendor = "kkyama77"
            copyright = "© 2026 kkyama77. Licensed under MIT."
            
            // JRE 同梱設定
            modules("java.sql", "java.desktop", "java.prefs", "jdk.unsupported")
            
            // Windows 設定
            windows {
                menuGroup = "Organize Photos"
                perUserInstall = true
                dirChooser = true
                shortcut = true
                upgradeUuid = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"
            }
            
            // macOS 設定
            macOS {
                bundleID = "com.kkyama77.organizephotos"
            }
            
            // Linux 設定
            linux {
                packageName = "organize-photos"
                debMaintainer = "kkyama77@example.com"
                menuGroup = "Graphics"
                appCategory = "Graphics"
            }
        }
    }
}

