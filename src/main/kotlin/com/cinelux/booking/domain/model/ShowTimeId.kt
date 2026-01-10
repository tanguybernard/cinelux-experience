package com.cinelux.booking.domain.model

@JvmInline
value class ShowTimeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ShowTimeId cannot be blank" }
    }
}
