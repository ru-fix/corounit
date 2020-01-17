package ru.fix.corounit.allure.example

import io.kotlintest.matchers.string.shouldNotBeBlank
import mu.KotlinLogging
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

class AirportSteps {
    suspend fun bookFlight(person: String) {
        log.info { "book flight for $person" }

        "validate person" {
            person.shouldNotBeBlank()
        }
    }
}