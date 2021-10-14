plugins {
    java
    kotlin("jvm")
}

dependencies {

    api(project(Projs.`corounit-engine`.asDependency))

    api(Libs.kotlin_jdk8)
    api(Libs.kotlin_stdlib)
    api(Libs.kotlin_reflect)



    api(Libs.mu_kotlin_logging)

    implementation(Libs.log4j_core)
    implementation(Libs.slf4j_over_log4j)

    testImplementation(Libs.kotlin_test)
    testImplementation(Libs.junit_jupiter_engine)
    testImplementation(Libs.junit_jupiter_api)
}

