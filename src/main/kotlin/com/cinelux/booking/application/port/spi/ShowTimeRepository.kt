package com.cinelux.booking.application.port.spi

import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import com.cinelux.booking.domain.model.ShowTimeReference

/**
 * SPI Port (driven) - Access showtime information
 */
interface ShowTimeRepository {
    fun findById(id: ShowTimeId): ShowTimeReference?

    fun getAllSeatsForShowTime(showTimeId: ShowTimeId): List<Seat>
}
