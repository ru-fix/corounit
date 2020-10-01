rootProject.name = "corounit"

for (project in listOf(
        "corounit-engine",
        "corounit-example",
        "corounit-allure",
        "corounit-allure-kotlin-plugin",
        "corounit-allure-gradle-plugin"
/* Ignore corounit-allure-example, since it depends on corounit-allure-gradle-plugin */
)) {
    include(project)
}