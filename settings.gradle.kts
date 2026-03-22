pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmp-ble"

include(":kmp-ble-codec")
include(":kmp-ble-quirks")
include(":sample")
include(":sample-android")
