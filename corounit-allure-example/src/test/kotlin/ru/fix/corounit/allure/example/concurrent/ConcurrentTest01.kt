package ru.fix.corounit.allure.example.concurrent

import io.qameta.allure.Feature
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import ru.fix.corounit.allure.invoke

private val log = KotlinLogging.logger { }

@Feature("concurrency")
class ConcurrentTest01 {

    @Test
    suspend fun `suspend test 0001`() {
        "step 0001"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0002`() {
        "step 0002"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0003`() {
        "step 0003"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0004`() {
        "step 0004"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0005`() {
        "step 0005"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0006`() {
        "step 0006"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0007`() {
        "step 0007"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0008`() {
        "step 0008"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0009`() {
        "step 0009"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0010`() {
        "step 0010"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0011`() {
        "step 0011"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0012`() {
        "step 0012"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0013`() {
        "step 0013"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0014`() {
        "step 0014"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0015`() {
        "step 0015"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0016`() {
        "step 0016"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0017`() {
        "step 0017"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0018`() {
        "step 0018"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0019`() {
        "step 0019"{
            delay(1_000)
        }
    }

    @Test
    suspend fun `suspend test 0020`() {
        "step 0020"{
            delay(1_000)
        }
    }
}