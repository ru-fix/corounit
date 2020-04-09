package ru.fix.corounit.allure.example

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DisabledMethodTest {
    @Disabled("disabled for example purpose")
    @Test
    suspend fun `disabled test method`(){
        (2 * 2).shouldBe(5)
    }
}