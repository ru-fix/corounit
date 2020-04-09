package ru.fix.corounit.allure.example

import io.kotlintest.matchers.numerics.shouldBeLessThan
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.parameterized
import ru.fix.corounit.allure.row

class ParameterizedMethodTest {
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