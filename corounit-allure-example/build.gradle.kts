
plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.allopen") version Vers.kotlin
    id("io.qameta.allure") version Vers.allure_plugin
}

allOpen{
    annotation("ru.fix.corounit.allure.Steps")
}


allure{
    version = Vers.allure_cli
    autoconfigure = false
    aspectjweaver = false
}


dependencies {

    api(project(Projs.`corounit-engine`.asDependency))
    api(project(Projs.`corounit-allure`.asDependency))

    api(Libs.kotlin_jdk8)
    api(Libs.kotlin_stdlib)
    api(Libs.kotlin_reflect)

    api(Libs.junit_engine)
    api(Libs.junit_api)

    api(Libs.mu_kotlin_logging)
    implementation(Libs.log4j_core)
    implementation(Libs.slf4j_over_log4j)
    testImplementation(Libs.kotlin_test)
}

tasks.create("allure-reporting"){
    description = """
        Takes allure-results directory 
    """.trimIndent()
    inputs.files("${project.buildDir}/allure-results")
    dependsOn("allureReport")

}

