package com.cinelux.booking.application.port.spi

import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId

/**
 * SPI Port (driven) - Access booking information
 */
interface BookingRepository {
    fun findBookedSeatsForShowTime(showTimeId: ShowTimeId): List<Seat>
}
