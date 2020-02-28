package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.numerics.shouldBeLessThan
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.*

private val log = KotlinLogging.logger { }


class AllureUsageTest {

    val airport = createStepClassInstance<AirportSteps>()

    @Test
    suspend fun `person books a flight`() {

        airport.`book flight for person`("Smith")

        AllureStep.attachment("test body attachment", "data: 42")

        "parent step" {

            AllureStep.attachment("parent step attachmetn", "data: 74")

            "sync child step" {
                true.shouldBeTrue()
            }

            coroutineScope {
                launch {
                    "async child step 1" {
                        delay(1000)
                        true.shouldBeTrue()
                    }
                }

                "mixed within async child step"{
                }

                launch {
                    "async child step 2"{
                        delay(1000)
                        true.shouldBeTrue()
                    }
                }
            }
        }
    }

    @Test
    suspend fun `test with parameters`() = parameterized(
            row(1, "one"),
            row(2, "two"),
            row(3, "three"),
            row(4, null)
    ) { number, text ->

        println("number $number is a $text")
        number.shouldBeLessThan(5)
    }
}
