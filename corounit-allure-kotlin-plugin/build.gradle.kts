import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "6.0.0"
}


dependencies {
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_compiler_embeddable)
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
}
//
//tasks {
//    named<ShadowJar>("shadowJar") {
//        dependencies {
//            exclude("org.jetbrains.kotlin:kotlin-jdk8")
//            exclude("org.jetbrains.kotlin:kotlin-stdlib")
//            exclude("org.jetbrains.kotlin:kotlin-compiler-embeddable")
//        }
//    }
//    build {
//        dependsOn(shadowJar)
//    }
//}