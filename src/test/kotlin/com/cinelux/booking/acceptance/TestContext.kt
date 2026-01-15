package com.cinelux.booking.acceptance

import com.cinelux.booking.acceptance.fake.FakeBookingRepository
import com.cinelux.booking.acceptance.fake.FakeShowTimeRepository
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId

/**
 * Shared test context for Cucumber step classes.
 * This singleton holds the shared state across all step definitions.
 * Reset in @Before of each scenario.
 */
object TestContext {
    lateinit var showTimeRepository: FakeShowTimeRepository
    lateinit var bookingRepository: FakeBookingRepository

    var currentShowTimeId: ShowTimeId? = null
    var allConfiguredSeats: List<Seat> = emptyList()

    fun reset() {
        showTimeRepository = FakeShowTimeRepository()
        bookingRepository = FakeBookingRepository()
        currentShowTimeId = null
        allConfiguredSeats = emptyList()
    }
}
