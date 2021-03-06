package ru.fix.corounit.allure.example

import io.kotlintest.matchers.string.shouldNotBeBlank
import mu.KotlinLogging
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

open class AirportSteps {

    open suspend fun `book flight for person`(person: String) {

        "book a seat on a flight for the person: $person" {
            person.shouldNotBeBlank()
        }
    }
}