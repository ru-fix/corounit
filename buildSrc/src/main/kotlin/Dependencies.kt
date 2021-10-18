object Vers {
    //Plugins
    const val gradle_release_plugin = "1.4.3"
    const val dokkav = "1.4.30"
    const val asciidoctor = "1.5.9.2"

    //Dependencies
    const val kotlin = "1.5.21"
    const val kotlin_coroutines = "1.5.2"
    const val junit = "5.6.0"

    const val log4j =  "2.12.0"

    const val allure_plugin = "2.9.6"
    const val allure_cli = "2.15.0"
    const val allure_java = "2.15.0"

    const val aspectj = "1.9.7"
    const val freefair_aspectj = "6.2.0"
}
object Libs {
    //Plugins
    const val gradle_release_plugin = "ru.fix:gradle-release-plugin:${Vers.gradle_release_plugin}"
    const val dokka_gradle_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Vers.dokkav}"
    const val asciidoctor = "org.asciidoctor:asciidoctor-gradle-plugin:${Vers.asciidoctor}"
    const val nexus_staging_plugin = "io.codearte.nexus-staging"
    const val nexus_publish_plugin = "de.marcphilipp.nexus-publish"

    //Dependencies
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Vers.kotlin}"
    const val kotlin_jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Vers.kotlin}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Vers.kotlin}"
    const val kotlinx_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Vers.kotlin_coroutines}"

    const val aspect_rt = "org.aspectj:aspectjrt:${Vers.aspectj}"
    const val aspect_weaver = "org.aspectj:aspectjweaver:${Vers.aspectj}"

    const val mu_kotlin_logging = "io.github.microutils:kotlin-logging:1.7.6"
    const val log4j_core = "org.apache.logging.log4j:log4j-core:${Vers.log4j}"
    const val slf4j_over_log4j = "org.apache.logging.log4j:log4j-slf4j-impl:${Vers.log4j}"

    //1.9.3 has a bug
    //https://github.com/mockk/mockk/issues/280
    const val mockk = "io.mockk:mockk:1.9.2"

    // Tests
    const val junit_api = "org.junit.jupiter:junit-jupiter-api:${Vers.junit}"
    const val junit_params = "org.junit.jupiter:junit-jupiter-params:${Vers.junit}"
    const val junit_engine = "org.junit.jupiter:junit-jupiter-engine:${Vers.junit}"



    const val kotlin_test = "io.kotlintest:kotlintest-runner-junit5:3.4.2"

    const val allure_model = "io.qameta.allure:allure-model:${Vers.allure_java}"
    const val allure_java_commons = "io.qameta.allure:allure-java-commons:${Vers.allure_java}"

    const val bytebuddy = "net.bytebuddy:byte-buddy:1.11.20"


}

enum class Projs{
    `corounit-engine`,
    `corounit-example`,
    `corounit-allure`,
    `corounit-allure-example`
    ;

    val asDependency get(): String = ":$name"
}




