package com.cinelux.booking.application.port.api

import com.cinelux.booking.domain.model.Seat

/**
 * API Port (driving) - Find available seats for a showtime
 */
interface FindAvailableSeatsUseCase {
    fun execute(query: FindAvailableSeatsQuery): AvailableSeatsResult
}

data class FindAvailableSeatsQuery(
    val showTimeId: String
)

data class AvailableSeatsResult(
    val availableSeats: List<Seat>,
    val bookedSeats: List<Seat>,
    val totalSeats: Int
) {
    val availableCount: Int get() = availableSeats.size
    val bookedCount: Int get() = bookedSeats.size

    fun hasAvailableSeats(): Boolean = availableSeats.isNotEmpty()

    fun isSeatAvailable(seat: Seat): Boolean = availableSeats.contains(seat)

    fun isSeatBooked(seat: Seat): Boolean = bookedSeats.contains(seat)
}
