package com.cinelux.booking.acceptance.fake

import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId

class FakeBookingRepository : BookingRepository {

    private val bookedSeatsByShowTime = mutableMapOf<ShowTimeId, MutableList<Seat>>()

    override fun findBookedSeatsForShowTime(showTimeId: ShowTimeId): List<Seat> {
        return bookedSeatsByShowTime[showTimeId] ?: emptyList()
    }

    fun bookSeat(showTimeId: ShowTimeId, seat: Seat) {
        bookedSeatsByShowTime.getOrPut(showTimeId) { mutableListOf() }.add(seat)
    }

    fun bookSeats(showTimeId: ShowTimeId, seats: List<Seat>) {
        bookedSeatsByShowTime.getOrPut(showTimeId) { mutableListOf() }.addAll(seats)
    }

    fun clear() {
        bookedSeatsByShowTime.clear()
    }
}
