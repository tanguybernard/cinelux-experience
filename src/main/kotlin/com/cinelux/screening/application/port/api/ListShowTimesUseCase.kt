package com.cinelux.screening.application.port.api

import java.time.DayOfWeek
import java.time.Instant

interface ListShowTimesUseCase {
    fun execute(query: ListShowTimesQuery): ListShowTimesResult
}

data class ListShowTimesQuery(
    val dayOfWeek: DayOfWeek
)

data class ListShowTimesResult(
    val showTimes: List<ShowTimeDto>
) {
    val count: Int get() = showTimes.size
    fun isEmpty(): Boolean = showTimes.isEmpty()
}

data class ShowTimeDto(
    val id: String,
    val movieTitle: String,
    val hallName: String,
    val startTime: Instant
)
