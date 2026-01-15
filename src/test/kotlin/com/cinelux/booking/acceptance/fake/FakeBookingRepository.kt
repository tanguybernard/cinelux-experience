package com.cinelux.booking.acceptance.fake

import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.domain.model.Booking
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId

class FakeBookingRepository : BookingRepository {

    private val bookedSeatsByShowTime = mutableMapOf<ShowTimeId, MutableList<Seat>>()
    private val bookings = mutableMapOf<String, Booking>()

    override fun findBookedSeatsForShowTime(showTimeId: ShowTimeId): List<Seat> {
        return bookedSeatsByShowTime[showTimeId] ?: emptyList()
    }

    override fun existsBookingForSeat(showTimeId: ShowTimeId, seat: Seat): Boolean {
        return bookedSeatsByShowTime[showTimeId]?.contains(seat) ?: false
    }

    override fun save(booking: Booking): Booking {
        bookings[booking.id.value] = booking
        bookedSeatsByShowTime.getOrPut(booking.showTimeId) { mutableListOf() }
            .add(booking.seat)
        return booking
    }

    // Test helper methods (not part of interface)
    fun bookSeat(showTimeId: ShowTimeId, seat: Seat) {
        bookedSeatsByShowTime.getOrPut(showTimeId) { mutableListOf() }.add(seat)
    }

    fun bookSeats(showTimeId: ShowTimeId, seats: List<Seat>) {
        bookedSeatsByShowTime.getOrPut(showTimeId) { mutableListOf() }.addAll(seats)
    }

    fun clear() {
        bookedSeatsByShowTime.clear()
        bookings.clear()
    }

    fun findById(bookingId: String): Booking? = bookings[bookingId]
}
