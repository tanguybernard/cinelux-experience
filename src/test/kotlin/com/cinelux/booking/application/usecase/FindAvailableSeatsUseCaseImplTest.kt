package com.cinelux.booking.application.usecase

import com.cinelux.booking.application.port.api.FindAvailableSeatsQuery
import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.application.port.spi.ShowTimeRepository
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import com.cinelux.booking.domain.model.ShowTimeReference
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FindAvailableSeatsUseCaseImplTest {

    @Test
    fun `should return available seats when some seats are booked`() {
        // Arrange
        val showTimeId = "show-123"
        val allSeats = listOf(
            Seat(row = "A", number = 1),
            Seat(row = "A", number = 2),
            Seat(row = "A", number = 3),
            Seat(row = "B", number = 1),
            Seat(row = "B", number = 2)
        )
        val bookedSeats = listOf(
            Seat(row = "A", number = 1),
            Seat(row = "B", number = 2)
        )

        val showTimeRepository = FakeShowTimeRepository(allSeats)
        val bookingRepository = FakeBookingRepository(bookedSeats)
        val useCase = FindAvailableSeatsUseCaseImpl(showTimeRepository, bookingRepository)

        // Act
        val result = useCase.execute(FindAvailableSeatsQuery(showTimeId))

        // Assert
        assertEquals(3, result.availableCount)
        assertEquals(2, result.bookedCount)
        assertEquals(5, result.totalSeats)
        assertTrue(result.hasAvailableSeats())
        assertEquals(
            listOf(Seat("A", 2), Seat("A", 3), Seat("B", 1)),
            result.availableSeats
        )
    }

    @Test
    fun `should return all seats as available when no seats are booked`() {
        // Arrange
        val showTimeId = "show-456"
        val allSeats = listOf(
            Seat(row = "A", number = 1),
            Seat(row = "A", number = 2)
        )

        val showTimeRepository = FakeShowTimeRepository(allSeats)
        val bookingRepository = FakeBookingRepository(emptyList())
        val useCase = FindAvailableSeatsUseCaseImpl(showTimeRepository, bookingRepository)

        // Act
        val result = useCase.execute(FindAvailableSeatsQuery(showTimeId))

        // Assert
        assertEquals(2, result.availableCount)
        assertEquals(0, result.bookedCount)
        assertTrue(result.hasAvailableSeats())
        assertEquals(allSeats, result.availableSeats)
    }

    @Test
    fun `should return no available seats when all seats are booked`() {
        // Arrange
        val showTimeId = "show-789"
        val allSeats = listOf(
            Seat(row = "C", number = 5),
            Seat(row = "C", number = 6)
        )

        val showTimeRepository = FakeShowTimeRepository(allSeats)
        val bookingRepository = FakeBookingRepository(allSeats)
        val useCase = FindAvailableSeatsUseCaseImpl(showTimeRepository, bookingRepository)

        // Act
        val result = useCase.execute(FindAvailableSeatsQuery(showTimeId))

        // Assert
        assertEquals(0, result.availableCount)
        assertEquals(2, result.bookedCount)
        assertFalse(result.hasAvailableSeats())
        assertTrue(result.availableSeats.isEmpty())
    }

    @Test
    fun `should return empty result when showtime has no seats`() {
        // Arrange
        val showTimeId = "show-empty"

        val showTimeRepository = FakeShowTimeRepository(emptyList())
        val bookingRepository = FakeBookingRepository(emptyList())
        val useCase = FindAvailableSeatsUseCaseImpl(showTimeRepository, bookingRepository)

        // Act
        val result = useCase.execute(FindAvailableSeatsQuery(showTimeId))

        // Assert
        assertEquals(0, result.availableCount)
        assertEquals(0, result.bookedCount)
        assertEquals(0, result.totalSeats)
        assertFalse(result.hasAvailableSeats())
    }
}

// Test doubles (fakes) for the ports
private class FakeShowTimeRepository(
    private val seats: List<Seat>
) : ShowTimeRepository {

    override fun findById(id: ShowTimeId): ShowTimeReference? {
        return ShowTimeReference(
            id = id,
            movieTitle = "Test Movie",
            startTime = Instant.now().plusSeconds(3600),
            hallId = "hall-1"
        )
    }

    override fun getAllSeatsForShowTime(showTimeId: ShowTimeId): List<Seat> = seats
}

private class FakeBookingRepository(
    private val bookedSeats: List<Seat>
) : BookingRepository {

    override fun findBookedSeatsForShowTime(showTimeId: ShowTimeId): List<Seat> = bookedSeats
}
