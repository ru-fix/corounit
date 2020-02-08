package ru.fix.corounit.allure.example

import io.kotlintest.matchers.string.shouldNotBeBlank
import mu.KotlinLogging
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

open class AirportSteps {

    suspend fun `book flight for person`(person: String) {
        log.info { "book flight for $person" }

        "validate person" {
            person.shouldNotBeBlank()
        }
    }
}