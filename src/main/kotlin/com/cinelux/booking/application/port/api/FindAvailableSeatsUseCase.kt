package com.cinelux.booking.application.port.api

import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.SeatSection

/**
 * API Port (driving) - Find available seats for a showtime
 */
interface FindAvailableSeatsUseCase {
    fun execute(query: FindAvailableSeatsQuery): AvailableSeatsResult
}

data class FindAvailableSeatsQuery(
    val showTimeId: String,
    val sectionFilter: SeatSection? = null
)

data class AvailableSeatsResult(
    val availableSeats: List<Seat>,
    val bookedSeats: List<Seat>,
    val totalSeats: Int
) {
    val availableCount: Int get() = availableSeats.size
    val bookedCount: Int get() = bookedSeats.size

    fun hasAvailableSeats(): Boolean = availableSeats.isNotEmpty()
}
