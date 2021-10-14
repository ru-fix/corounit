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



    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.4")

    testImplementation(Libs.kotest_assertions)
    testImplementation(Libs.log4j_slf4j_impl)
    testImplementation(Libs.log4j_kotlin)
    testImplementation(Libs.junit_jupiter_engine)
    testImplementation(Libs.junit_jupiter_api)
}

tasks {
    withType<Test> {
        useJUnitPlatform()

        maxParallelForks = 10

        testLogging {
            events(org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED, org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED, org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED)
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

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
}