import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import ru.fix.corounit.allure.gradle.plugin.CorounitAllureExtension

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("ru.fix:corounit-allure-gradle-plugin:1.0-SNAPSHOT")
    }
}



plugins {
    java
    kotlin("jvm") version "1.4.10"
    `maven-publish`
    id("io.qameta.allure") version "2.8.1"
}

apply(plugin = "ru.fix.corounit.allure")


configure<CorounitAllureExtension>{
}


allure{
    version = "2.13.1"
}


repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}


dependencies {
    api("ru.fix:corounit-engine:1.0-SNAPSHOT")
    api("ru.fix:corounit-allure:1.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")

    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")

    implementation("org.apache.logging.log4j:log4j-core:2.12.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.12.0")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
}

tasks {
    create("allure-reporting") {
        description = """
        Takes allure-results directory 
    """.trimIndent()
        inputs.files("${project.buildDir}/allure-results")
        dependsOn("allureReport")

    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    withType<Test> {
        useJUnitPlatform() {
            val INCLUDE_TAGS = "includeTags"
            val EXCLUDE_TAGS = "excludeTags"

            if (project.hasProperty(INCLUDE_TAGS)) {
                includeTags(project.properties[INCLUDE_TAGS] as String)
            }
            if (project.hasProperty(EXCLUDE_TAGS)) {
                excludeTags(project.properties[EXCLUDE_TAGS] as String)
            }
        }

        maxParallelForks = 10

        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }

    }
}

