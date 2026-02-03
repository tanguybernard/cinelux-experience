package com.cinelux.screening.application.usecase

import com.cinelux.screening.application.port.api.ListShowTimesQuery
import com.cinelux.screening.application.port.api.ListShowTimesResult
import com.cinelux.screening.application.port.api.ListShowTimesUseCase
import com.cinelux.screening.application.port.api.ShowTimeDto
import com.cinelux.screening.application.port.spi.ShowTimeRepository
import com.cinelux.screening.domain.model.ShowTime

class ListShowTimesUseCaseImpl(
    private val showTimeRepository: ShowTimeRepository
) : ListShowTimesUseCase {

    override fun execute(query: ListShowTimesQuery): ListShowTimesResult {
        val showTimes = showTimeRepository.findByDayOfWeek(query.dayOfWeek)

        return ListShowTimesResult(
            showTimes = showTimes.map { it.toDto() }
        )
    }

    private fun ShowTime.toDto() = ShowTimeDto(
        id = id.value,
        movieTitle = movieTitle,
        hallName = hall.name,
        startTime = startTime
    )
}
