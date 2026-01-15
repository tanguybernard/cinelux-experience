package com.cinelux.booking.application.usecase

import com.cinelux.booking.application.port.api.BookSeatCommand
import com.cinelux.booking.application.port.api.BookSeatResult
import com.cinelux.booking.application.port.api.BookSeatUseCase
import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.application.port.spi.ShowTimeRepository
import com.cinelux.booking.domain.model.Booking
import com.cinelux.booking.domain.model.BookingId
import com.cinelux.booking.domain.model.CustomerId
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import java.util.UUID

class BookSeatUseCaseImpl(
    private val bookingRepository: BookingRepository,
    private val showTimeRepository: ShowTimeRepository
) : BookSeatUseCase {

    override fun execute(command: BookSeatCommand): BookSeatResult {
        val showTimeId = ShowTimeId(command.showTimeId)

        // 1. Validate showtime exists
        val showTime = showTimeRepository.findById(showTimeId)
            ?: return BookSeatResult.ShowTimeNotFound(command.showTimeId)

        // 2. Create seat value object
        val seat = Seat(row = command.seatRow, number = command.seatNumber)

        // 3. Validate seat exists in the hall
        val hallSeats = showTimeRepository.getAllSeatsForShowTime(showTimeId)
        if (seat !in hallSeats) {
            return BookSeatResult.SeatNotInHall(
                seat = seat.displayName(),
                hallId = showTime.hallId
            )
        }

        // 4. Check for double-booking
        if (bookingRepository.existsBookingForSeat(showTimeId, seat)) {
            return BookSeatResult.SeatAlreadyBooked(seat = seat.displayName())
        }

        // 5. Create and save booking
        val booking = Booking.create(
            id = BookingId(UUID.randomUUID().toString()),
            customerId = CustomerId(command.customerId),
            seat = seat,
            showTimeId = showTimeId
        )

        val savedBooking = bookingRepository.save(booking)

        // 6. Return success
        return BookSeatResult.Success(
            bookingId = savedBooking.id.value,
            seat = savedBooking.seat.displayName(),
            movieTitle = showTime.movieTitle
        )
    }
}
