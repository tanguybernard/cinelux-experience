package com.cinelux.booking.domain.model

import java.time.Instant

/**
 * Reference to a ShowTime entity from the Screening bounded context.
 * Booking context doesn't own the full ShowTime - just references it.
 */
data class ShowTimeReference(
    val id: ShowTimeId,
    val movieTitle: String,
    val startTime: Instant,
    val hallId: String
)
