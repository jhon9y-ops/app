pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "webrtc-kotlin-p2p"

include(":shared")
include(":server")
include(":client-js")
include(":client-android")
