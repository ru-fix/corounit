# Corounit
Junit Test Engine for suspendable tests

Corounit can run thousands test classes and methods concurrently using small fixed size thread pool.  


```kotlin
class UsageTest {
    @Test
    suspend fun `first suspend test`() {
        delay(1_000)
        log.info { "simple suspend test" }
    }

    @Test
    suspend fun `second suspend test`() {
        delay(1_000)
        log.info { "simple suspend test" }
    }
}
``` 