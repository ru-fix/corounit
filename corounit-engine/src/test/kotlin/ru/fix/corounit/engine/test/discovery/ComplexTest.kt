package ru.fix.corounit.engine.test.discovery

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test

/**
 * interface with default methods generate extra class
 * that could lead to reflection failure of kotlin reflection methods
 *
 * https://github.com/spring-projects/spring-framework/issues/20548
 * ```
 * java.lang.UnsupportedOperationException: This class is an internal synthetic class generated by the Kotlin compiler, such as an anonymous class for a lambda, a SAM wrapper, a callable reference, etc. It's not a Kotlin class or interface, so the reflection library has no idea what declarations does it have. Please use Java reflection to inspect this class: class ru.fix.corounit.engine.test.discovery.NonTestInterface$DefaultImpls
 * at kotlin.reflect.jvm.internal.KClassImpl.reportUnresolvedClass(KClassImpl.kt:307)
 * at kotlin.reflect.jvm.internal.KClassImpl.access$reportUnresolvedClass(KClassImpl.kt:43)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data$descriptor$2.invoke(KClassImpl.kt:53)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data$descriptor$2.invoke(KClassImpl.kt:44)
 * at kotlin.reflect.jvm.internal.ReflectProperties$LazySoftVal.invoke(ReflectProperties.java:92)
 * at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:31)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data.getDescriptor(KClassImpl.kt)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data$nestedClasses$2.invoke(KClassImpl.kt:97)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data$nestedClasses$2.invoke(KClassImpl.kt:44)
 * at kotlin.reflect.jvm.internal.ReflectProperties$LazySoftVal.invoke(ReflectProperties.java:92)
 * at kotlin.reflect.jvm.internal.ReflectProperties$Val.getValue(ReflectProperties.java:31)
 * at kotlin.reflect.jvm.internal.KClassImpl$Data.getNestedClasses(KClassImpl.kt)
 * at kotlin.reflect.jvm.internal.KClassImpl.getNestedClasses(KClassImpl.kt:237)
 * at kotlin.reflect.full.KClasses.getCompanionObject(KClasses.kt:51)
 * at kotlin.reflect.jvm.ReflectJvmMapping.getKotlinFunction(ReflectJvmMapping.kt:124)
 * ```
 */

class ComplexTest(override val arg: String) : NonTestInterface {
    suspend fun doOtherWork(){
        delay(0)
    }
}

interface NonTestInterface{
    val arg: String

    suspend fun doWork(){
        delay(1)
    }
}

class PlainTest{
    @Test
    suspend fun plainTestMethod(){
        delay(1)
    }
}