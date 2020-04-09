package ru.fix.corounit.allure

import io.kotlintest.shouldBe
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test

class RepeatUntilSuccessTest {

    @Test
    suspend fun `Делаем повторную попытку для упавших шагов`(){
        var attempt = 0

        repeatUntilSuccess {
            "Шаг, который работает с третьей попытки"{
                attempt++
                "Текущая попытка: $attempt"{}
                "Ждем 1 сек"{
                    delay(1000)
                }
                "Проверяем не наступила ли 3-ая попытка. Текущая попытка: $attempt"{
                    attempt.shouldBe(3)
                }
            }
        }

        repeatUntilSuccess(timeout = 20_000, delay = 10_000) {
            "Шаг, который работает с первой попытки"{
                "Ждем 1 сек"{
                    delay(1000)
                }
                1.shouldBe(1)
            }
        }
    }
}