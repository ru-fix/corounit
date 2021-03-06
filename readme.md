# Corounit
Junit Test Engine for suspendable tests  
Corounit can run thousands test cases concurrently using small amount of threads.  
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/corounit-engine.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)


- [Corounit](#corounit)
  * [Standard JUnit Test Engine approach](#standard-junit-test-engine-approach)
  * [Corounit Test Engine suspendable test approach](#corounit-test-engine-suspendable-test-approach)
  * [Test class instance](#test-class-instance)
  * [Suspendable and non suspendable test methods](#suspendable-and-non-suspendable-test-methods)
  * [Test beforeAll and afterAll](#test-beforeall-and-afterall)
  * [Test beforeEach and afterEach](#test-beforeeach-and-aftereach)
  * [Corounit plugins and listeners](#corounit-plugins-and-listeners)
  * [Allure integration](#allure-integration)
    + [Allure steps](#allure-steps)
    + [Parameterized tests](#parameterized-tests)
  * [Corounit Properties](#corounit-properties)
  * [JUnit friendship](#junit-friendship)
    + [@Test annotation](#-test-annotation)
    + [@Tag annotation](#-tag-annotation)
    + [@TestInstance annotation](#-testinstance-annotation)
    + [@BeforeAll, @BeforeEach, @AfterEach, @AfterAll annotations](#-beforeall---beforeeach---aftereach---afterall-annotations)
    + [@ParameterizedTest annotation not supported yet.](#-parameterizedtest-annotation-not-supported-yet)
  * [Allure reporting](#allure-reporting)


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
corounit.testinstance.lifecycle.default=per_class
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

## Test class instance

By default corounit uses new test class instance for each method invocation. 
As JUnit does. 
You can override this behaviour via `@TestInstance(PER_CLASS)` annotation. 
Or use `corounit.testinstance.lifecycle.default=per_class` property.

## Suspendable and non suspendable test methods
Corounit scan for `suspend` methods annotated with `@Test` annotation.
If test class has both types of test methods, corounit will launch suspendable ones and default JUnit Test Engine will launch default test methods.

```kotlin
class TestClass{
    //runs by JUnit
    @Test
    fun `my regular test`(){...}

    //runs by Corounit
    @Test
    suspend fun `my suspend test`(){...}   
}
```

## Test beforeAll and afterAll
Corounit follows convention over configuration approach. 
Methods `beforeAll` and `afterAll` do not need special annotations `@BeforeAll`, `@AfterAll`. 
They will be invoked before and after all test methods of the test class.
However annotations `@BeforeAll` and `@AfterAll` works just fine.

```kotlin
@TestInstance(PER_CLASS)
class TestClass{
    suspend fun beforeAll(){...}
    suspend fun afterAll(){...}

    @Test
    suspend fun `my suspend test`(){...}   
}
```
You can mark these method by annotations too.
```kotlin
@TestInstance(PER_CLASS)
class TestClass{
    @BeforeAll
    suspend fun setUp(){...}
    @AfterAll
    suspend fun tearDown(){...}

    @Test
    suspend fun `my suspend test`(){...}   
}
``` 

In case of default `@TestInstance(PER_METHOD)` behaviour, 
you have to define beforeAll and afterAll methods in companion object.
```kotlin
class TestClass{
    companion object{
        suspend fun beforeAll(){...}
        suspend fun afterAll(){...}
    }

    @Test
    suspend fun `my suspend test`(){...}   
}
``` 
## Test beforeEach and afterEach
You can name `beforeEach` and `afterEach`. 
Then they will be invoked before and after each of test methods.
Or you can annotate methods with `@BeforeEach` or `@AfterEach` annotation.
```kotlin
class TestClass{
    suspend fun beforeEach(){...}
    suspend fun afterEach(){...}

    @Test
    suspend fun `my test`(){...}   
}
```
```kotlin
class TestClass{
    @BeforeEach
    suspend fun createResources(){...}
    
    @AfterEach    
    suspend fun destroyResources(){...}

    @Test
    suspend fun `my test`(){...}   
}
```

## Corounit plugins and listeners
Corounit Test Engine provide extension point for test execution lifecycle.  
This extension point called `CorounitPlugin`.  
You can use it to Listen for test run events or override coroutine context and behaviour.

There are two options to add extension point into your project:  
  
* Create `CorounitConfig` object in your test module package.
* Or use JVM ServiceLoader mechanism. 
Define `ru.fix.corounit.engine.CorounitPlugin` entry within META-INF/services.
You can tak a look at  `corounit-allure` module.

Object `CorounitConfig` should implement `CorounitPlugin` interface or `CorounitPluginsProvider` interface.  
Put this object in subpackage of any of the test classes.

Override any methods of `CorounitPlugin` that you want to.  
Or define list of plugins by impemening `CorounitPluginsProvider` interface.  
For example, you can start mocking server before all test cases and shutdown after all test cases complete.

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
You can provide several plugins from single CorounitConfig object by implementing `CorounitPluginsProvider` interface.
```kotlin
package base.pacakge.of.the.project

object CorounitConfig: CorounitPluginsProvider {

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
Example of a listener that logs test execution results
```kotlin
class TestResultLogger: CorounitPlugin{
    override suspend fun afterTestMethod(testMethodContext: CoroutineContext, thr: Throwable?) {
        val testClass = testMethodContext[TestClassContextElement]!!.testClass
        val testMethod = testMethodContext[TestMethodContextElement]!!.testMethod
        if(thr == null){
            log.info { "Test $testClass : $testMethod passed" }
        }else {
            log.info { "Test $testClass : $testMethod failed with exception: $thr" }
        }
    }
    override suspend fun skipTestMethod(testMethodContext: CoroutineContext, reason: String) {
        val testClass = testMethodContext[TestClassContextElement]!!.testClass
        val testMethod = testMethodContext[TestMethodContextElement]!!.testMethod
        log.info { "Test $testClass : $testMethod skipped due to $reason" }
    }
}
```

## Allure integration
`corounit-allure` provides api to enrich test with Allure step description. 

### Allure steps
There are three options to create Allure steps that will be visible in Allure Report
 * Annotate `suspend` method with `@Step` annotation.
 * Annotate class with `@Step` annotation.
 * Create dynamic subclasses via `AllureAspect.newAspectedInstanceViaSubtyping`
 * Create step via string extension blocks `"step name"{...}`  
 
#### Allure step via annotation 
 Be aware that it should be `ru.fix.corounit.allure.Step` annotation.
 ```kotlin
import ru.fix.corounit.allure.Step

@Step
suspend fun `purchase a ticket`(date:  LocalDate){
    ...
}
```
Be aware that `StepAnnotationAspect` from `corounit-allure` should be activated during your build.
```kotlin
plugins{
    id("io.freefair.aspectj.base") version Vers.freefair_aspectj
    id("io.freefair.aspectj.post-compile-weaving") version Vers.freefair_aspectj
}
dependencies{
    testAspect("ru.fix:corounit-allure:${Vers.corounit}")
    testImplementation(Libs.aspect_weaver)
}
``` 
`@Step` annotated aproach currently does not works for methods with recursion.

In IntelliJ IDEA ascpects defined this way could be ingored.   
In order to launch tests from IDE in gradle project switch executor to gradle:  
https://www.jetbrains.com/help/idea/work-with-tests-in-gradle.html 

#### Allure step via dynamic subclasses
`AllureAspect.createStepClassInstance` creates subclass and wrap `open` `suspend` functions into a step.
```kotlin
val airport = createStepClassInstance<AirportSteps>()
airport.`book flight for person`(person)
```    

#### Allure step via string extension
String steps allow clarifying test case and will be present in Allure report.
```kotlin
@Test
suspend fun `user travel`(){
    "Purchase a flight ticket"{
        ...
    }
    "Booking a hotel"{
        ...
    }
}
```

### Parameterized tests

```kotlin
class TestClass{
    @Test
    suspend fun `my test`() = parameterized(
        row(1, "one"),
        row(2, "two"),
        row(3, "three")){ number, text -> 
        ...
    }   
}
```

## Corounit Properties
You can add corounit properties within default JUnit property file at `src/test/resources/junit-platform.properties`:

* `corounit.execution.parallelism=4` 
 How many threads corounite engine will use to execute tests.
* `corounit.testinstance.lifecycle.default=per_class` 
 By default corounit will create new instance of test class for each method invocation. Default value is calculated based on number of CPU.
 This property changes that behaviour so corounit will create single test class instance
 and will use same instance for all method invocation.
 Explicit behaviour can be set via `@TestInstance(PER_CLASS)` test class annotation. Default value is `per_method`.

## JUnit friendship

### @Test annotation
Corounit Engine will look for `suspendable` test methods marked with `@Test` annotation and run them. 
All non suspendable regular methods marked with `@Test` will stay untouched.
Default JUnit Engine will launch them after Corounit Engine finish running suspendable tests.  

### @Tag annotation
`@Tag` annotation works in a same way as in default JUnit Engine.  
As in regular gradle junit suite you can configure gradle to receive tag filter from command line.
```kotlin
tasks {
    withType<Test> {
        useJUnitPlatform(){
            val INCLUDE_TAGS = "includeTags"
            val EXCLUDE_TAGS = "excludeTags"

            if(project.hasProperty(INCLUDE_TAGS)) {
                includeTags(project.properties[INCLUDE_TAGS] as String)
            }
            if(project.hasProperty(EXCLUDE_TAGS)) {
                excludeTags(project.properties[EXCLUDE_TAGS] as String)
            }
        }

        maxParallelForks = 10

        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showStandardStreams = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}
```
```shell script
gradle -p corounit-example clean test -PincludeTags="slow"
``` 

### @TestInstance annotation
Works in similar way

### @BeforeAll, @BeforeEach, @AfterEach, @AfterAll annotations
Works in similar way.
Can be declared in Companion object.
In corounit annotations are optional.
Suspendable methods supported.
See details in related paragraph.  

### @ParameterizedTest annotation not supported yet.
You can use `parameterized` function from `allure` module.
```kotlin
@Test
suspend fun `test with parameters`() = parameterized(
        row(1, "one"),
        row(2, "two"),
        row(3, "three"),
        row(4, null)
) { number, text ->
    println("number $number is a $text")
}
```

## Allure reporting
Corounit provides allure (http://allure.qatools.ru/) reporting integration via `corounit-allure` plugin.

Add `allure.properties` file in `src/test/resources` directory.
```properties
allure.results.directory=build/allure-results
```
Add `corounit-allure` dependency to your project. 

If you are using gradle we recomend to use `io.qameta.allure` plugin. 
Take a look at `corounit-allure-example` module.
