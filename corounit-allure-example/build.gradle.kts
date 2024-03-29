
plugins {
    java
    kotlin("jvm")
    id("io.qameta.allure") version Vers.allure_plugin
    id("io.qameta.allure-adapter") version Vers.allure_plugin

//    id("io.freefair.aspectj.base") version Vers.freefair_aspectj
//    id("io.freefair.aspectj.post-compile-weaving") version Vers.freefair_aspectj
}


allure{
    adapter{
        version.set(Vers.allure_cli)
        allureJavaVersion.set(Vers.allure_java)

        frameworks{
            junit5{
                enabled.set(false)
            }
        }
    }

}

//aspectj{
//    version.set(Vers.aspectj)
//}

dependencies {

    api(project(Projs.`corounit-engine`.asDependency))
    api(project(Projs.`corounit-allure`.asDependency))

    api(Libs.kotlin_jdk8)
    api(Libs.kotlin_stdlib)
    api(Libs.kotlin_reflect)

    api(Libs.junit_engine)
    api(Libs.junit_api)

    api(Libs.mu_kotlin_logging)

/*
Kotlin 1.5 is not supported by aspectj.
So compile time aspect injection via aspectj post compilation weaving
methods marked with [ru.fix.corounit.allure.Step] annotation
currently not supported
 */
//    aspect(project(Projs.`corounit-allure`.asDependency))

    implementation(Libs.log4j_core)
    implementation(Libs.slf4j_over_log4j)

    testImplementation(Libs.kotlin_test)
//    testAspect(project(Projs.`corounit-allure`.asDependency))
//    testImplementation(Libs.aspect_weaver)


}

tasks.create("allure-reporting"){
    description = """
        Takes allure-results directory 
    """.trimIndent()
    inputs.files("${project.buildDir}/allure-results")
    dependsOn("allureReport")

}

