import org.asciidoctor.gradle.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI


buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath(Libs.gradle_release_plugin)
        classpath(Libs.asciidoctor)
    }
}

val repositoryUser: String? by project
val repositoryPassword: String? by project
val repositoryUrl: String? by project

plugins {
    base
    kotlin("jvm") version Vers.kotlin apply false
    `maven-publish`
    id("org.asciidoctor.convert") version Vers.asciidoctor
}

apply {
    plugin("ru.fix.gradle.release")
}

subprojects {
    group = "ru.fix"

    apply {
        plugin("maven-publish")
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven(url = "https://repo.spring.io/milestone/")
        maven(url = "http://artifactory.vasp/artifactory/libs-release/")
    }

    project.afterEvaluate {
        publishing {
            publications {
                if (components.names.contains("java")) {
                    logger.info("Register java artifact for project: ${project.name}")

                    val sourcesJar by tasks.creating(Jar::class) {
                        classifier = "sources"
                        from("src/main/java")
                        from("src/main/kotlin")
                    }

                    register("${project.name}-mvnPublication", MavenPublication::class) {
                        from(components["java"])
                        artifact(sourcesJar)
                    }
                }
            }

            repositories {
                maven {
                    credentials {
                        username = "$repositoryUser"
                        password = "$repositoryPassword"
                    }

                    name = "repo"
                    url = URI("$repositoryUrl")
                }
            }
        }
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        withType<Test> {
            useJUnitPlatform()

            maxParallelForks = 10

            testLogging {
                events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
                showStandardStreams = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}

tasks {
    withType<AsciidoctorTask> {
        sourceDir = project.file("asciidoc")
        resources(closureOf<CopySpec> {
            from("asciidoc")
            include("**/*.png")
        })
        doLast {
            copy {
                from(outputDir.resolve("html5"))
                into(project.file("docs"))
                include("**/*.html", "**/*.png")
            }
        }
    }
}


