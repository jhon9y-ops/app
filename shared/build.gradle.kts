plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    // Target para Web / Kotlin/JS
    js(IR) {
        browser()
    }

    // Target para JVM / Android
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
    }
}