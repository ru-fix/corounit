package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.AllureAspect
import ru.fix.corounit.allure.AllureStep
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }


class AllureUsageTest {

    val airport = AllureAspect.newAspectedInstanceViaSubtyping(AirportSteps::class.java) as AirportSteps

    @Test
    suspend fun `suspend test`() {
        airport.bookFlight("Smith")
        log.info { "Flight booked" }

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
}
