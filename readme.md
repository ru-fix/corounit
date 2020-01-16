# Corounit
Junit Test Engine for suspendable tests

[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/corounit.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

Corounit can run thousands test classes and methods concurrently using small fixed size thread pool.  

Standard JUnit test engine can be configured to use Thread per Method approach.
https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution

```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
junit.jupiter.execution.parallel.mode.default = concurrent
junit.jupiter.execution.parallel.mode.classes.default = concurrent
```

```kotlin
object MyServiceHttpClient{
    fun doAction1() {...}
    fun doAction2() {...}
    ...
    fun doAction100(){...}

    fun isActionResultAvailable(): Boolean {...}
    fun getActionResult(): Result {...}
}

class LongRunningTest {
    @Test
    fun `action1 test`() {
        client.doAction1()
        while(!client.isActionResultAvailable()){
            Thread.sleep(1_000)
        }
        client.getActionResult().shouldBe(...)
    }

    @Test
    fun `action2 test`() {
        client.doAction2()
        while(!client.isActionResultAvailable()){
            Thread.sleep(1_000)
        }
        client.getActionResult().shouldBe(...)
    }
    ...
}
```

Corounit coroutines approach
```properties
corounit.execution.parallelism=4
```

```kotlin
object MyServiceHttpClient{
    suspend fun doAction1() {...}
    suspend fun doAction2() {...}
    suspend fun isActionResultAvailable(): Boolean {...}
    suspend fun getActionResult(): Result {...}
}

class LongRunningTest {
    @Test
    suspend fun `action1 test`() {
        client.doAction1()
        while(!client.isActionResultAvailable()){
            delay(1_000)
        }
        client.getActionResult().shouldBe(...)
    }

    @Test
    suspend fun `action2 test`() {
        client.doAction2()
        while(!client.isActionResultAvailable()){
            delay(1_000)
        }
        client.getActionResult().shouldBe(...)
    }
    ...
}
```  

