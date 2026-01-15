package com.cinelux.booking.domain.model

@JvmInline
value class BookingId(val value: String) {
    init {
        require(value.isNotBlank()) { "BookingId cannot be blank" }
    }
}
