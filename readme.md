# Corounit
Junit Test Engine for suspendable tests  
Corounit can run thousands test cases concurrently using small amount of threads.  
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/corounit-engine.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)


* [Standard JUnit Test Engine approach](#standard-junit-test-engine-approach)
* [Corounit Test Engine suspendable test approach](#corounit-test-engine-suspendable-test-approach)
* [Corounit plugins](#corounit-plugins)
* [Test class](#test-class)
* [JUnit friendship](#junit-friendship)
* [Allure integration](#allure-integration)


## Standard JUnit Test Engine approach
Standard JUnit test engine execute test classes and their methods within classical Threads. 
In order to increase speed of testing phase JUnit engine can be configured to run test clasess in parallel in different threads.
Or even run each test method of same test class in different threads.
See https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution

Given this configuration JUnit Test Engine will use thread pool of size 4 and run all test classes and test methods in parallel.

```properties
# junit-platform.properties
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
In given example `action4 test` will occupy `Thread 4`. 
The problem here is that `action4 test` can spent most of the time waiting for response from tested server.

![](docs/thread-test-threads.png?raw=true)

Due to memory restriction we can not spawn 1000 threads for each class and method of our test suites. So we have to use limited number of threads and can not launch all our test cases simultaneously.



## Corounit Test Engine suspendable test approach
It is common for integration test to spent most of the time waiting for server  response. 
With Kotlin coroutines it is possible do not waste thread resources on waiting. 
With suspendable test methods we can start thousands of tests simultaneously without OutOfMemoryException. All of them will start almost simultaneously and require only small amount of threads to run. 

![](docs/thread-test-coroutines.png?raw=true)

Default JUnit Engine requires `corounit` library in class path in order to be able to start suspendable methods.
You can use default `junit-platform.properties` file in test resources to specify limited number of threads that will be used by Corounit Test Engine.
      
```properties
# junit-platform.properties
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

## Corounit plugins
Corounit Test Engine provide extension point for test execution lifecycle.
* Use JVM ServiceLoader mechanism. You can tak a look at  `corounit-allure` module.
* Create `CorounitConfig` object in your test module package.

Object `CorounitConfig` should implement `CorounitPlugin` interface.  
Put this object in subpackage of any of the test classes.

Override any methods that you want to. 
For example you can start mocking server before all test cases and shutdown after all test cases complete.

```kotlin
package base.pacakge.of.the.project

object CorounitConfig: CorounitPlugin {

    override suspend fun beforeAllTestClasses(globalContext: CoroutineContext): CoroutineContext {
        // do custom initialization here,
        // like starting mock server 
        // that will be used by several different test classes        
        return super.beforeAllTestClasses(globalContext)
    }
    override suspend fun afterAllTestClasses(globalContext: CoroutineContext) {
        // shutdow mock server after all test classes completion
        super.afterAllTestClasses(globalContext)
    }
    
    // CorounitPlugin has other handy test lifecycle methods waiting for you to override.  
    ...
}
```
## Test class
Corounit follows convention over configuration approach. 
Methods `beforeAll` and `afterAll` do not need special annotations `@BeforeAll`, `@AfterAll`. 
They will be invoked before and after all test methods of the test class.
However annotations `@BeforeAll` and `@AfterAll` works just fine.

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

## JUnit friendship
Corounit Engine will look for `suspendable` test methods marked with `@Test` annotation and run them. 
All non suspendable regular methods marked with `@Test` will stay untouched.
Default JUnit Engine will launch them after Corounit Engine finish running suspendable tests.   


## Allure integration
Corounit provides allure (http://allure.qatools.ru/) reporting integration via `corounit-allure` plugin.

Add `allure.properties` file in `src/test/resources` directory.
```properties
allure.results.directory=build/allure-results
```
Add `corounit-allure` dependency to your project. 

If you are using gradle we recomend to use `io.qameta.allure` plugin. 
Take a look at `corounit-allure-example` module.
