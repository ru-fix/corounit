# Corounit
Junit Test Engine for suspendable tests  
Corounit can run thousands test cases concurrently using small amount of threads.  
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/corounit-engine.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)


## Standard JUnit Test Engine approach
Standard JUnit test engine execute test classes and their methods within classical Threads. 
In order to improve testing phase you JUnit engine can be configured to run test clasess in parallel in different threads.
Or even run test methods of same test class in different threads.
See https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution

Given this configuration JUnit Test Engine will use thread pool of size 4 and run all test classes and test methods in parallel.

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
![](docs/thread-test.png?raw=true)

## Corounit Test Engine suspendable test approach
It is common for integration test to spent most of the time waiting for server  response. 
With Kotlin coroutines it is possible do not waste thread resources on waiting. 
This way we can start thousands of tests simultaneously without OutOfMemoryException.      
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

## Plugin
Corounit provide extension point for test execution lifecycle.  
Create object `CorounitConfig` that implement `CorounitPlugin` interface.  
Put this object in subpackage of any of the test classes.

```kotlin
package base.pacakge.of.the.project

object CorounitConfig: CorounitPlugin {

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        // do custom initialization here,
        // like starting mock server that will be used by several different test classes        
        return super.beforeAllTestClasses(globalContext)
    }
    ...
}
```
## TestClass
Methods beforeAll and afterAll do not need special annotations `@BeforeAll`, `@AfterAll`. 
They will be invoked before and after all test methods of the test class.

```kotlin
class TestClass{
    suspend fun beforeAll(){...}
    suspend fun afterAll(){...}

    @Test
    suspend fun `my suspend test`(){...}   
}
```
You can mark these method by annotations too.
```kotlin
class TestClass{
    @BeforeAll
    suspend fun setUp(){...}
    @AfterAll
    suspend fun tearDown(){...}

    @Test
    suspend fun `my suspend test`(){...}   
}
``` 



## Allure integration
Corounit provides allure (http://allure.qatools.ru/) reporting integration.  

Add `allure.properties` file in `src/test/resources` directory.
```properties
allure.results.directory=build/allure-results
```