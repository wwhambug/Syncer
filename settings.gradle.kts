// settings.gradle.kts (Project 루트)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EchoSyncExtension"
include(":echosync")  // 모듈 이름: echosync (build.gradle.kts가 위치한 디렉토리명과 일치)