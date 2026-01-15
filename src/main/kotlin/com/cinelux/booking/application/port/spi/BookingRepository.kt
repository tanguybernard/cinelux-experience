package com.cinelux.booking.application.port.spi

import com.cinelux.booking.domain.model.Booking
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId

/**
 * SPI Port (driven) - Access booking information
 */
interface BookingRepository {
    fun findBookedSeatsForShowTime(showTimeId: ShowTimeId): List<Seat>

    fun existsBookingForSeat(showTimeId: ShowTimeId, seat: Seat): Boolean

    fun save(booking: Booking): Booking
}
