package ru.fix.corounit.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class UseDependencyInjectionByCorounitConfigTest {

    lateinit var data: String

    /**
     * Method called by [CorounitConfig.createTestClassInstance]
     */
    fun injectData(data: String){
        this.data = data
    }

    @Test
    open suspend fun `corounit config used as a dependency injection for this test class`(){
        CorounitConfig.beforeAllInvoked.get().shouldBeTrue()
        data.shouldBe("injected data")
    }

}