plugins{
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenLocal()
    jcenter()
    gradlePluginPortal()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("https://dl.bintray.com/mipt-npm/dataforge")
    maven("https://dl.bintray.com/mipt-npm/kscience")
    maven("https://dl.bintray.com/mipt-npm/dev")
}

kotlin{
    jvm()
    js(IR){
        browser()
        nodejs()
    }

    sourceSets{
        commonMain{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
            }
        }
    }
}

//plugins {
//    id("ru.mipt.npm.mpp")
//    id("ru.mipt.npm.node")
//    id("ru.mipt.npm.native")
//}
//
//kscience {
//    useSerialization()
//}

description = "Meta definition and basic operations on meta"