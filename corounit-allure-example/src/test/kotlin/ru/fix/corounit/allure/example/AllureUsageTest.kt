package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import kotlinx.coroutines.*
import mu.KotlinLogging
import ru.fix.corounit.allure.Allure
import ru.fix.corounit.allure.AllureAspect
import ru.fix.corounit.allure.invoke
import org.junit.jupiter.api.Test

private val log = KotlinLogging.logger { }


class AllureUsageTest {

    val airport = AllureAspect.newAspectedInstanceViaSubtyping(AirportSteps::class.java) as AirportSteps

    @Test
    suspend fun `suspend test`() {
        airport.bookFlight("Smith")
        log.info { "Flight booked" }

        Allure.attachment("test body attachment", "data: 42")

        "parent step" {

            Allure.attachment("parent step attachmetn", "data: 74")

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
