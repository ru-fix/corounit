package ru.fix.corounit.allure.example

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.*
import ru.fix.corounit.engine.Listeners

@Listeners([ExampleListener::class, ExampleListener2::class])
class ListenersUsageTest {

    @Disabled("disabled for example purpose")
    @Test
    suspend fun `disabled test method`(){
        (2 * 2).shouldBe(5)
    }

    @Test
    suspend fun `succeed test method`(){
        (2 * 2).shouldBe(4)
    }
}
