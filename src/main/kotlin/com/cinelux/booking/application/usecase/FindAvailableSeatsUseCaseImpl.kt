package com.cinelux.booking.application.usecase

import com.cinelux.booking.application.port.api.AvailableSeatsResult
import com.cinelux.booking.application.port.api.FindAvailableSeatsQuery
import com.cinelux.booking.application.port.api.FindAvailableSeatsUseCase
import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.application.port.spi.ShowTimeRepository
import com.cinelux.booking.domain.model.ShowTimeId

class FindAvailableSeatsUseCaseImpl(
    private val showTimeRepository: ShowTimeRepository,
    private val bookingRepository: BookingRepository
) : FindAvailableSeatsUseCase {

    override fun execute(query: FindAvailableSeatsQuery): AvailableSeatsResult {
        val showTimeId = ShowTimeId(query.showTimeId)

        val allSeats = showTimeRepository.getAllSeatsForShowTime(showTimeId)
        val bookedSeats = bookingRepository.findBookedSeatsForShowTime(showTimeId)

        val availableSeats = allSeats - bookedSeats.toSet()

        return AvailableSeatsResult(
            availableSeats = availableSeats,
            bookedSeats = bookedSeats,
            totalSeats = allSeats.size
        )
    }
}
