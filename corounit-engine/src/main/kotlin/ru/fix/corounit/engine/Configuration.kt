package ru.fix.corounit.engine

import org.junit.jupiter.api.TestInstance
import org.junit.platform.engine.ConfigurationParameters
import java.util.concurrent.ForkJoinPool

class Configuration(configurationParameters: ConfigurationParameters) {

    val concurrentTestClassesLimit = configurationParameters.get("corounit.execution.concurrent.test.classes.limit")
        .map { it?.toInt() }
        .orElse(null)

    val concurrentTestMethodsLimit = configurationParameters.get("corounit.execution.concurrent.test.methods.limit")
        .map { it?.toInt() }
        .orElse(null)

    val parallelism = configurationParameters.get("corounit.execution.parallelism")
            .map { it?.toInt() }
            .orElse(Math.max(ForkJoinPool.getCommonPoolParallelism(), 2))!!

    val testInstanceLifecycle = configurationParameters.get("corounit.testinstance.lifecycle.default")
            .let { defaultLifecycleFromProperty ->
                if (defaultLifecycleFromProperty.isPresent &&
                        defaultLifecycleFromProperty.get() == "per_class") {
                    TestInstance.Lifecycle.PER_CLASS
                } else {
                    TestInstance.Lifecycle.PER_METHOD
                }
            }
}