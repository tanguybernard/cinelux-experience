package com.cinelux.booking.domain.model

import java.time.Instant

/**
 * Aggregate root representing a confirmed seat reservation.
 * In CineLux, bookings are free and immediately confirmed.
 */
data class Booking(
    val id: BookingId,
    val customerId: CustomerId,
    val seat: Seat,
    val showTimeId: ShowTimeId,
    val bookedAt: Instant
) {
    companion object {
        fun create(
            id: BookingId,
            customerId: CustomerId,
            seat: Seat,
            showTimeId: ShowTimeId,
            bookedAt: Instant = Instant.now()
        ): Booking {
            return Booking(
                id = id,
                customerId = customerId,
                seat = seat,
                showTimeId = showTimeId,
                bookedAt = bookedAt
            )
        }
    }
}
