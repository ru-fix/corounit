
plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    kotlin("kapt")
}

gradlePlugin {
    plugins {
        create("corounitAllurePlugin") {
            id = "ru.fix.corounit.allure" // users will do `apply plugin: "ru.fix.corounit.allure"`
            implementationClass = "ru.fix.corounit.allure.gradle.plugin.CorounitAllureGradlePlugin"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${Vers.kotlin}")
    implementation(Libs.kotlin_jdk8)
    implementation(Libs.kotlin_stdlib)
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")

}

tasks.withType<ProcessResources> {
    filesMatching("corounit-allure-gradle-plugin.properties") {
        expand(project.properties)
    }
}