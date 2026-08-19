pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "webrtc-kotlin-p2p"

// Módulos essenciais que rodam tanto no seu PC quanto na Nuvem
include(":shared")
include(":server")

// Se NÃO estiver rodando na nuvem (Railway/Render), carrega o Android e o JS localmente
if (System.getenv("RAILWAY_STATIC_URL") == null && System.getenv("RENDER") == null) {
    include(":client-js")
    include(":client-android")
}
