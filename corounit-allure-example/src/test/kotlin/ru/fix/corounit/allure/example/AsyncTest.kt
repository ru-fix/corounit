package ru.fix.corounit.allure.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.invoke

class AsyncTest {

    @Test
    suspend fun `async test`() {
        "sync step" {
            true.shouldBeTrue()
        }

        coroutineScope {
            launch {
                "async child step 1" {
                    delay(500)
                    true.shouldBeTrue()
                }
            }

            "sync child step 2"{

            }

            launch {
                "async child step 3"{
                    delay(500)
                    true.shouldBeTrue()
                }
            }
        }
    }
}