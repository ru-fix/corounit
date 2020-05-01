
plugins {
    java
    kotlin("jvm")
    id("io.freefair.aspectj.base") version "5.0.1"
    id("io.freefair.aspectj.post-compile-weaving") version "5.0.1"
}

aspectj{
    version.set(Vers.aspectj)
}


dependencies {

    api(project(Projs.`corounit-engine`.asDependency))

    api(Libs.kotlin_jdk8)
    api(Libs.kotlin_stdlib)
    api(Libs.kotlin_reflect)

    api(Libs.junit_engine)
    api(Libs.junit_api)

    api(Libs.mu_kotlin_logging)

    api(Libs.bytebuddy)
    api(Libs.allure_model)
    api(Libs.allure_java_commons)

    implementation(Libs.aspect_rt)

    implementation(Libs.log4j_core)
    implementation(Libs.slf4j_over_log4j)

    testImplementation(Libs.kotlin_test)
    testImplementation(Libs.mockk)

}

