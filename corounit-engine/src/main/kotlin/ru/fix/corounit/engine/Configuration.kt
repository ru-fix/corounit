package ru.fix.corounit.engine

import org.junit.jupiter.api.TestInstance
import org.junit.platform.engine.ConfigurationParameters
import java.util.concurrent.ForkJoinPool

class Configuration(configurationParameters: ConfigurationParameters) {
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