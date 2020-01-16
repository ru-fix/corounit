rootProject.name = "corounit"

for (project in listOf(
        "corounit-engine",
        "corounit-example",
        "corounit-allure",
        "corounit-allure-example"
)) {
    include(project)
}