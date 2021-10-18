package ru.fix.corounit.allure.example

import io.kotlintest.matchers.string.shouldNotBeBlank
import mu.KotlinLogging
import ru.fix.corounit.allure.Step
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

open class SecuritySteps {

    @Step
    open suspend fun `check person`(person: String) {
        "ensure that person is a law abiding citizen" {
            person.shouldNotBeBlank()
        }
    }
}