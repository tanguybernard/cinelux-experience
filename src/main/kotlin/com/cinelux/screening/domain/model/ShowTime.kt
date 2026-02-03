package com.cinelux.screening.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class ShowTime(
    val id: ShowTimeId,
    val movieTitle: String,
    val hall: Hall,
    val startTime: Instant,
    val dayOfWeek: DayOfWeek
) {
    init {
        require(movieTitle.isNotBlank()) { "Movie title cannot be blank" }
    }

    companion object {
        fun create(
            id: ShowTimeId,
            movieTitle: String,
            hall: Hall,
            startTime: Instant
        ): ShowTime {
            return ShowTime(
                id = id,
                movieTitle = movieTitle,
                hall = hall,
                startTime = startTime,
                dayOfWeek = startTime.atZone(ZoneId.systemDefault()).dayOfWeek
            )
        }
    }
}
