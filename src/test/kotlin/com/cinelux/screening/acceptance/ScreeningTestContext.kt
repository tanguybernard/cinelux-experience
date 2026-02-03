package com.cinelux.screening.acceptance

import com.cinelux.screening.acceptance.fake.FakeShowTimeRepository

object ScreeningTestContext {
    lateinit var showTimeRepository: FakeShowTimeRepository

    fun reset() {
        showTimeRepository = FakeShowTimeRepository()
    }
}
