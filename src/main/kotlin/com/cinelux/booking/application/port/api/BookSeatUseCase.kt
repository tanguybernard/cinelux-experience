package com.cinelux.booking.application.port.api

/**
 * API Port (driving) - Book a specific seat for a showtime
 */
interface BookSeatUseCase {
    fun execute(command: BookSeatCommand): BookSeatResult
}

data class BookSeatCommand(
    val customerId: String,
    val showTimeId: String,
    val seatRow: String,
    val seatNumber: Int
)

sealed interface BookSeatResult {
    data class Success(
        val bookingId: String,
        val seat: String,
        val movieTitle: String
    ) : BookSeatResult

    data class SeatAlreadyBooked(
        val seat: String
    ) : BookSeatResult

    data class ShowTimeNotFound(
        val showTimeId: String
    ) : BookSeatResult

    data class SeatNotInHall(
        val seat: String,
        val hallId: String
    ) : BookSeatResult
}
