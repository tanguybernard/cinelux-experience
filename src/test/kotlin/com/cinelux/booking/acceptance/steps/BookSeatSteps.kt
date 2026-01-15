package com.cinelux.booking.acceptance.steps

import com.cinelux.booking.acceptance.TestContext
import com.cinelux.booking.application.port.api.BookSeatCommand
import com.cinelux.booking.application.port.api.BookSeatResult
import com.cinelux.booking.application.port.api.BookSeatUseCase
import com.cinelux.booking.application.usecase.BookSeatUseCaseImpl
import com.cinelux.booking.domain.model.Seat
import com.cinelux.booking.domain.model.ShowTimeId
import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class BookSeatSteps {

    private lateinit var bookSeatUseCase: BookSeatUseCase
    private var currentCustomerId: String? = null
    private var result: BookSeatResult? = null

    @Before(order = 1)
    fun setup() {
        // TestContext is reset by ViewAvailableSeatsSteps @Before(order = 0)
        bookSeatUseCase = BookSeatUseCaseImpl(
            TestContext.bookingRepository,
            TestContext.showTimeRepository
        )
    }

    // Booking-specific steps
    @Given("I am customer {string}")
    fun iAmCustomer(customerName: String) {
        currentCustomerId = "customer-$customerName"
    }

    @Given("seat {string} is already booked for {string} at {string}")
    fun seatIsAlreadyBookedFor(seatCode: String, movie: String, time: String) {
        val showTimeId = TestContext.currentShowTimeId ?: ShowTimeId("show-$movie-$time")
        val seat = parseSeatCode(seatCode)
        TestContext.bookingRepository.bookSeat(showTimeId, seat)
    }

    @When("I book seat {string} for {string} at {string}")
    fun iBookSeatFor(seatCode: String, movie: String, time: String) {
        // Always construct showTimeId from movie/time to support "non-existent showtime" scenarios
        val showTimeId = "show-$movie-$time"
        val seat = parseSeatCode(seatCode)

        result = bookSeatUseCase.execute(
            BookSeatCommand(
                customerId = currentCustomerId!!,
                showTimeId = showTimeId,
                seatRow = seat.row,
                seatNumber = seat.number
            )
        )
    }

    @Then("the booking should be confirmed")
    fun theBookingShouldBeConfirmed() {
        assertThat(result).isInstanceOf(BookSeatResult.Success::class.java)
    }

    @Then("I should receive a booking confirmation with:")
    fun iShouldReceiveBookingConfirmationWith(dataTable: DataTable) {
        assertThat(result).isInstanceOf(BookSeatResult.Success::class.java)
        val success = result as BookSeatResult.Success

        val expected = dataTable.asMap(String::class.java, String::class.java)
        expected["Seat"]?.let { assertThat(success.seat).isEqualTo(it) }
        expected["Movie"]?.let { assertThat(success.movieTitle).isEqualTo(it) }
    }

    @Then("the booking should fail with {string}")
    fun theBookingShouldFailWith(expectedReason: String) {
        when (expectedReason) {
            "Seat already booked" -> assertThat(result).isInstanceOf(BookSeatResult.SeatAlreadyBooked::class.java)
            "Seat not in hall" -> assertThat(result).isInstanceOf(BookSeatResult.SeatNotInHall::class.java)
            "Showtime not found" -> assertThat(result).isInstanceOf(BookSeatResult.ShowTimeNotFound::class.java)
            else -> throw IllegalArgumentException("Unknown reason: $expectedReason")
        }
    }

    @And("seat {string} should be booked for {string} at {string}")
    fun seatShouldBeBookedFor(seatCode: String, movie: String, time: String) {
        val showTimeId = TestContext.currentShowTimeId ?: ShowTimeId("show-$movie-$time")
        val seat = parseSeatCode(seatCode)
        val isBooked = TestContext.bookingRepository.existsBookingForSeat(showTimeId, seat)
        assertThat(isBooked).isTrue()
    }

    private fun parseSeatCode(seatCode: String): Seat {
        val row = seatCode.substring(0, 1)
        val number = seatCode.substring(1).toInt()
        return Seat(row = row, number = number)
    }
}
