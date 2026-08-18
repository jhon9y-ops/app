plugins {
    // Define o ecossistema Kotlin Multiplatform e Serialização
    kotlin("multiplatform") version "1.9.23" apply false
    kotlin("plugin.serialization") version "1.9.23" apply false
    
    // Plugins do Android Application e Kotlin Android
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}