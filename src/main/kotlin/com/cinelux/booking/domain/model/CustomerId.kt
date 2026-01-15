package com.cinelux.booking.domain.model

@JvmInline
value class CustomerId(val value: String) {
    init {
        require(value.isNotBlank()) { "CustomerId cannot be blank" }
    }
}
