package ru.fix.corounit.allure.example

import mu.KotlinLogging
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.AllureStep
import ru.fix.corounit.allure.createStepClassInstance
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }


class BookFlightTest {

    /**
     * Uses runtime time aspect injection by generating AirportSteps superclass
     * Class should have open suspend methods to became Step method
     */
    val airport = createStepClassInstance<AirportSteps>()

    /**
     * Uses compile time aspect injection via aspectj post compilation weaving
     * methods marked with [ru.fix.corounit.allure.Step] annotation
     */
    val security = SecuritySteps()

    @Test
    suspend fun `person books a flight`() {

        val person = "Smith"

        security.`check person`(person)

        airport.`book flight for person`(person)

        AllureStep.attachment("ticket", "ticket-data: 42")

        "parent step" {

            AllureStep.attachment("parent step attachment", "sub-ticket-data: 74")

        }
    }
}
