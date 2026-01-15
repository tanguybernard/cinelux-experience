package com.cinelux.booking.acceptance.steps

import com.cinelux.booking.acceptance.fake.FakeBookingRepository
import com.cinelux.booking.acceptance.fake.FakeShowTimeRepository
import com.cinelux.booking.application.port.api.AvailableSeatsResult
import com.cinelux.booking.application.port.api.FindAvailableSeatsQuery
import com.cinelux.booking.application.port.api.FindAvailableSeatsUseCase
import com.cinelux.booking.application.usecase.FindAvailableSeatsUseCaseImpl
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import com.cinelux.booking.domain.model.ShowTimeReference
import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant
import java.time.temporal.ChronoUnit

class ViewAvailableSeatsSteps {

    private lateinit var showTimeRepository: FakeShowTimeRepository
    private lateinit var bookingRepository: FakeBookingRepository
    private lateinit var findAvailableSeatsUseCase: FindAvailableSeatsUseCase

    private var currentShowTimeId: ShowTimeId? = null
    private var allConfiguredSeats: List<Seat> = emptyList()
    private var result: AvailableSeatsResult? = null

    @Before
    fun setup() {
        showTimeRepository = FakeShowTimeRepository()
        bookingRepository = FakeBookingRepository()
        findAvailableSeatsUseCase = FindAvailableSeatsUseCaseImpl(showTimeRepository, bookingRepository)
    }

    @Given("{string} is showing at {string} in {string}")
    fun movieIsShowingAtIn(movieTitle: String, time: String, hall: String) {
        val showTimeId = ShowTimeId("show-$movieTitle-$time")
        currentShowTimeId = showTimeId

        val showTime = ShowTimeReference(
            id = showTimeId,
            movieTitle = movieTitle,
            startTime = Instant.now().plus(1, ChronoUnit.DAYS),
            hallId = hall
        )

        showTimeRepository.addShowTime(showTime, emptyList())
    }

    @And("the hall has the following seats:")
    fun theHallHasTheFollowingSeats(dataTable: DataTable) {
        val seats = mutableListOf<Seat>()

        dataTable.asMaps().forEach { row ->
            val rowLetter = row["Row"]!!
            val numbersRange = row["Numbers"]!!

            val (start, end) = parseNumbersRange(numbersRange)

            for (number in start..end) {
                seats.add(Seat(row = rowLetter, number = number))
            }
        }

        allConfiguredSeats = seats
        currentShowTimeId?.let { showTimeId ->
            val existingShowTime = showTimeRepository.findById(showTimeId)!!
            showTimeRepository.addShowTime(existingShowTime, seats)
        }
    }

    @Given("the following seats are already booked for {string} at {string}:")
    fun theFollowingSeatsAreAlreadyBookedFor(movie: String, time: String, dataTable: DataTable) {
        val showTimeId = currentShowTimeId ?: ShowTimeId("show-$movie-$time")

        dataTable.asMaps().forEach { row ->
            val seatCode = row["Seat"]!!
            val seat = parseSeatCode(seatCode)
            bookingRepository.bookSeat(showTimeId, seat)
        }
    }

    @Given("all seats are already booked for {string} at {string}")
    fun allSeatsAreAlreadyBookedFor(movie: String, time: String) {
        val showTimeId = currentShowTimeId ?: ShowTimeId("show-$movie-$time")

        allConfiguredSeats.forEach { seat ->
            bookingRepository.bookSeat(showTimeId, seat)
        }
    }

    @When("I view available seats for {string} at {string}")
    fun iViewAvailableSeatsFor(movie: String, time: String) {
        val showTimeId = currentShowTimeId?.value ?: "show-$movie-$time"

        result = findAvailableSeatsUseCase.execute(
            FindAvailableSeatsQuery(showTimeId = showTimeId)
        )
    }

    @Then("I should see {int} available seats")
    fun iShouldSeeAvailableSeats(expectedCount: Int) {
        assertThat(result).isNotNull
        assertThat(result!!.availableCount).isEqualTo(expectedCount)
    }

    @And("all seats should be marked as available")
    fun allSeatsShouldBeMarkedAsAvailable() {
        assertThat(result).isNotNull
        assertThat(result!!.bookedCount).isEqualTo(0)
        assertThat(result!!.availableCount).isEqualTo(result!!.totalSeats)
    }

    @And("seats {string}, {string}, {string} should be marked as unavailable")
    fun seatsShouldBeMarkedAsUnavailable(seat1Code: String, seat2Code: String, seat3Code: String) {
        assertThat(result).isNotNull

        val seat1 = parseSeatCode(seat1Code)
        val seat2 = parseSeatCode(seat2Code)
        val seat3 = parseSeatCode(seat3Code)

        assertThat(result!!.isSeatBooked(seat1)).isTrue()
        assertThat(result!!.isSeatBooked(seat2)).isTrue()
        assertThat(result!!.isSeatBooked(seat3)).isTrue()
    }

    @And("seat {string} should be marked as available")
    fun seatShouldBeMarkedAsAvailable(seatCode: String) {
        assertThat(result).isNotNull

        val seat = parseSeatCode(seatCode)
        assertThat(result!!.isSeatAvailable(seat)).isTrue()
    }

    @And("I should receive a message {string}")
    fun iShouldReceiveAMessage(expectedMessage: String) {
        assertThat(result).isNotNull

        if (expectedMessage == "No seats available") {
            assertThat(result!!.hasAvailableSeats()).isFalse()
        }
    }

    private fun parseNumbersRange(range: String): Pair<Int, Int> {
        val parts = range.split("-")
        return if (parts.size == 2) {
            parts[0].toInt() to parts[1].toInt()
        } else {
            parts[0].toInt() to parts[0].toInt()
        }
    }

    private fun parseSeatCode(seatCode: String): Seat {
        val row = seatCode.substring(0, 1)
        val number = seatCode.substring(1).toInt()
        return Seat(row = row, number = number)
    }
}
