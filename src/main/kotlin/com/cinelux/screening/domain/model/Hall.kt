package com.cinelux.screening.domain.model

data class Hall(
    val id: String,
    val name: String
) {
    init {
        require(id.isNotBlank()) { "Hall id cannot be blank" }
        require(name.isNotBlank()) { "Hall name cannot be blank" }
    }
}
