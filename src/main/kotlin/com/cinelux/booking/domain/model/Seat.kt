package com.cinelux.booking.domain.model

data class Seat(
    val row: String,
    val number: Int,
    val section: SeatSection
) {
    init {
        require(row.matches(Regex("^[A-Z]$"))) {
            "Row must be a single uppercase letter (A-Z), got: $row"
        }
        require(number in 1..50) {
            "Seat number must be between 1 and 50, got: $number"
        }
    }

    fun displayName(): String = "$row$number"

    override fun toString(): String = displayName()
}
