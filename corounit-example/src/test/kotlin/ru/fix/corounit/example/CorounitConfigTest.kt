package ru.fix.corounit.example

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class CorounitConfigTest {

    lateinit var data: String
    fun injectData(data: String){
        this.data = data
    }

    @Test
    open suspend fun testPluginInjection(){
        CorounitConfig.beforeAllInvoked.get().shouldBeTrue()
        data.shouldBe("injected data")
    }

}