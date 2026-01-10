---
paths: src/test/**/*.kt, src/test/**/*.feature
---

# BDD Testing Strategy with Cucumber

## Philosophy

**Behavior-Driven Development (BDD)** makes tests readable by domain experts, product owners, and developers. Tests written in Gherkin serve as **living documentation** that stays synchronized with the actual system behavior.

### Key Benefits for CineLux

1. **Living Documentation**: Feature files document system behavior in plain language
2. **Shared Understanding**: Domain experts validate scenarios
3. **Ubiquitous Language**: Tests use exact terms from domain model
4. **Regression Safety**: Scenarios catch breaking changes
5. **Onboarding**: New developers understand system by reading features

---

## Testing Layers with BDD

### Test Pyramid with BDD

```
           E2E Features (few)
         /                  \
    Acceptance Features (some)
  /                            \
Domain Unit Tests (many)
```

**Ratio**: 60% Domain Unit Tests, 30% Acceptance Features, 10% E2E Features

### Layer Breakdown

1. **Domain Unit Tests** (Kotlin/JUnit)
   - Pure business logic
   - Entity validation
   - Fast, no frameworks

2. **Acceptance Features** (Cucumber)
   - Use case scenarios
   - Application layer behavior
   - Mock adapters

3. **E2E Features** (Cucumber + Spring Boot Test)
   - Full stack integration
   - Real database
   - REST API testing

---

## Maven Dependencies

Add to `pom.xml`:

```xml
<properties>
    <cucumber.version>7.15.0</cucumber.version>
</properties>

<dependencies>
    <!-- Cucumber for Kotlin -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-spring</artifactId>
        <version>${cucumber.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ for fluent assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Directory Structure

```
src/test/
├── kotlin/com/cinelux/booking/
│   ├── domain/                         # Domain unit tests (JUnit)
│   │   ├── model/
│   │   │   ├── BookingTest.kt
│   │   │   └── SeatTest.kt
│   │   └── service/
│   │       └── SeatAvailabilityCheckerTest.kt
│   │
│   ├── acceptance/                     # BDD Acceptance Tests
│   │   ├── CucumberAcceptanceTest.kt   # Test runner
│   │   ├── CucumberConfig.kt           # Configuration
│   │   └── steps/                      # Step definitions
│   │       ├── BookingSteps.kt
│   │       ├── SeatAvailabilitySteps.kt
│   │       └── CommonSteps.kt
│   │
│   └── e2e/                            # BDD E2E Tests
│       ├── CucumberE2ETest.kt
│       └── steps/
│           └── BookingApiSteps.kt
│
└── resources/
    ├── features/                       # Gherkin feature files
    │   ├── booking/
    │   │   ├── book-seat.feature
    │   │   ├── confirm-booking.feature
    │   │   ├── cancel-booking.feature
    │   │   └── seat-availability.feature
    │   └── scenarios/
    │       └── complete-booking-journey.feature
    │
    └── cucumber.properties             # Cucumber configuration
```

---

## Writing Feature Files

### Use Ubiquitous Language

Feature files MUST use the exact terms from your domain model.

**File**: `src/test/resources/features/booking/book-seat.feature`

```gherkin
Feature: Book a Cinema Seat
  As a cinema customer
  I want to book a seat for a movie showing
  So that I can guarantee my place at the cinema

  Background:
    Given the movie "The Matrix" is showing at "19:00" in "Hall 1"
    And the hall has the following seat layout:
      | Row | Numbers    | Section  |
      | A   | 1-10       | STANDARD |
      | B   | 1-10       | STANDARD |
      | C   | 1-8        | VIP      |

  Scenario: Successfully book an available seat
    Given I am customer "Alice"
    And seat "A5" is available for the "19:00" showing
    When I book seat "A5" for "The Matrix" at "19:00"
    Then my booking should be created with status "PENDING"
    And seat "A5" should be reserved for the "19:00" showing
    And I should receive a booking confirmation with booking ID

  Scenario: Cannot book an already booked seat
    Given I am customer "Alice"
    And customer "Bob" has already booked seat "A5" for "The Matrix" at "19:00"
    When I attempt to book seat "A5" for "The Matrix" at "19:00"
    Then the booking should fail with reason "Seat already booked"
    And seat "A5" should still be booked by "Bob"

  Scenario: Cannot book a seat for a past showing
    Given I am customer "Alice"
    And "The Matrix" was showing yesterday at "19:00"
    When I attempt to book seat "A5" for that past showing
    Then the booking should fail with reason "Cannot book seats for past showtimes"

  Scenario: Book VIP seat with price premium
    Given I am customer "Alice"
    And the base ticket price is 10.00 EUR
    When I book VIP seat "C4" for "The Matrix" at "19:00"
    Then my booking should be created
    And the booking price should be 15.00 EUR
    And the booking should note the VIP section
```

**File**: `src/test/resources/features/booking/confirm-booking.feature`

```gherkin
Feature: Confirm a Booking
  As a cinema system
  I want to confirm bookings after successful payment
  So that seats are guaranteed for paying customers

  Background:
    Given customer "Alice" has booked seat "A5" for "The Matrix" at "19:00"
    And the booking status is "PENDING"

  Scenario: Confirm a pending booking
    When the payment is successfully processed
    And the booking is confirmed
    Then the booking status should be "CONFIRMED"
    And the confirmation timestamp should be recorded
    And the customer should receive a confirmation notification

  Scenario: Cannot confirm an already confirmed booking
    Given the booking is already "CONFIRMED"
    When I attempt to confirm the booking again
    Then the confirmation should fail with message "Only pending bookings can be confirmed"

  Scenario: Cannot confirm a cancelled booking
    Given the booking was "CANCELLED"
    When I attempt to confirm the booking
    Then the confirmation should fail with message "Only pending bookings can be confirmed"
```

**File**: `src/test/resources/features/booking/cancel-booking.feature`

```gherkin
Feature: Cancel a Booking
  As a cinema customer
  I want to cancel my booking
  So that I can free up the seat and potentially get a refund

  Scenario: Cancel a pending booking
    Given customer "Alice" has booked seat "A5" for "The Matrix" at "19:00"
    And the booking status is "PENDING"
    When Alice cancels the booking
    Then the booking status should be "CANCELLED"
    And the cancellation timestamp should be recorded
    And seat "A5" should become available again

  Scenario: Cancel a confirmed booking
    Given customer "Alice" has a "CONFIRMED" booking for seat "A5"
    When Alice cancels the booking
    Then the booking status should be "CANCELLED"
    And a refund should be initiated

  Scenario: Cannot cancel an already cancelled booking
    Given customer "Alice" had a booking that is already "CANCELLED"
    When Alice attempts to cancel it again
    Then the cancellation should fail with message "Booking already cancelled"
```

**File**: `src/test/resources/features/scenarios/complete-booking-journey.feature`

```gherkin
Feature: Complete Booking Journey
  As a cinema customer
  I want to complete the entire booking process
  So that I can watch a movie

  Scenario: Happy path - Book, confirm, and attend
    Given I am customer "Alice"
    And "The Matrix" is showing tomorrow at "19:00" in "Hall 1"
    And seat "A5" is available

    When I book seat "A5" for "The Matrix" at "19:00"
    Then my booking should be in "PENDING" status

    When I complete the payment
    Then my booking should be "CONFIRMED"
    And I should receive a booking confirmation email
    And I should receive a booking confirmation with QR code

    When I arrive at the cinema with my QR code
    Then the staff should be able to verify my booking
    And I should be allowed entry to "Hall 1"

  Scenario: Book and cancel before payment
    Given I am customer "Alice"
    And I have booked seat "A5" for "The Matrix" at "19:00"
    And my booking is "PENDING"

    When I cancel the booking before payment
    Then my booking should be "CANCELLED"
    And seat "A5" should become available for other customers
    And I should not be charged
```

---

## Writing Step Definitions

### Acceptance Test Step Definitions

Step definitions for acceptance tests mock the adapters and test use case logic.

**File**: `src/test/kotlin/com/cinelux/booking/acceptance/steps/BookingSteps.kt`

```kotlin
package com.cinelux.booking.acceptance.steps

import com.cinelux.booking.application.port.input.*
import com.cinelux.booking.application.port.output.*
import com.cinelux.booking.application.usecase.BookSeatUseCaseImpl
import com.cinelux.booking.domain.model.*
import io.cucumber.java.en.*
import org.assertj.core.api.Assertions.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class BookingSteps {

    private lateinit var bookingRepository: FakeBookingRepository
    private lateinit var showTimeRepository: FakeShowTimeRepository
    private lateinit var bookSeatUseCase: BookSeatUseCase

    private var currentCustomerId: String? = null
    private var currentShowTimeId: String? = null
    private var bookingResult: BookingResult? = null
    private var lastCreatedBooking: Booking? = null

    @Before
    fun setup() {
        bookingRepository = FakeBookingRepository()
        showTimeRepository = FakeShowTimeRepository()
        bookSeatUseCase = BookSeatUseCaseImpl(bookingRepository, showTimeRepository)
    }

    @Given("I am customer {string}")
    fun i_am_customer(customerName: String) {
        currentCustomerId = "customer-$customerName"
    }

    @Given("the movie {string} is showing at {string} in {string}")
    fun the_movie_is_showing_at(movieTitle: String, time: String, hall: String) {
        val showTime = ShowTimeReference(
            id = ShowTimeId("show-$movieTitle-$time"),
            movieTitle = movieTitle,
            startTime = Instant.now().plus(1, ChronoUnit.DAYS),
            hallId = hall
        )
        showTimeRepository.add(showTime)
        currentShowTimeId = showTime.id.value
    }

    @Given("seat {string} is available for the {string} showing")
    fun seat_is_available_for_showing(seatCode: String, time: String) {
        // No booking exists = seat is available
        // Nothing to do, just document the precondition
    }

    @Given("customer {string} has already booked seat {string} for {string} at {string}")
    fun customer_has_already_booked_seat(
        customerName: String,
        seatCode: String,
        movie: String,
        time: String
    ) {
        val (row, number) = parseSeatCode(seatCode)
        val seat = Seat(row, number, SeatSection.STANDARD)
        val showTime = showTimeRepository.findByMovieAndTime(movie, time)
            ?: throw IllegalStateException("ShowTime not found")

        val booking = Booking.create(
            id = BookingId("booking-${System.nanoTime()}"),
            customer = CustomerId("customer-$customerName"),
            seat = seat,
            showTime = showTime
        ).confirm()

        bookingRepository.save(booking)
    }

    @When("I book seat {string} for {string} at {string}")
    fun i_book_seat(seatCode: String, movie: String, time: String) {
        val (row, number) = parseSeatCode(seatCode)

        val command = BookSeatCommand(
            customerId = currentCustomerId!!,
            showTimeId = currentShowTimeId!!,
            row = row,
            seatNumber = number
        )

        bookingResult = bookSeatUseCase.execute(command)

        if (bookingResult is BookingResult.Success) {
            val bookingId = (bookingResult as BookingResult.Success).bookingId
            lastCreatedBooking = bookingRepository.findById(BookingId(bookingId))
        }
    }

    @When("I attempt to book seat {string} for {string} at {string}")
    fun i_attempt_to_book_seat(seatCode: String, movie: String, time: String) {
        i_book_seat(seatCode, movie, time)
    }

    @Then("my booking should be created with status {string}")
    fun my_booking_should_be_created_with_status(expectedStatus: String) {
        assertThat(bookingResult).isInstanceOf(BookingResult.Success::class.java)
        assertThat(lastCreatedBooking).isNotNull
        assertThat(lastCreatedBooking!!.status).isEqualTo(BookingStatus.valueOf(expectedStatus))
    }

    @Then("seat {string} should be reserved for the {string} showing")
    fun seat_should_be_reserved(seatCode: String, time: String) {
        val (row, number) = parseSeatCode(seatCode)
        val seat = Seat(row, number, SeatSection.STANDARD)
        val showTime = showTimeRepository.findByTime(time)!!

        val isBooked = bookingRepository.existsConfirmedBooking(seat, showTime.id)
        assertThat(isBooked).isTrue()
    }

    @Then("I should receive a booking confirmation with booking ID")
    fun i_should_receive_booking_confirmation() {
        assertThat(bookingResult).isInstanceOf(BookingResult.Success::class.java)
        val bookingId = (bookingResult as BookingResult.Success).bookingId
        assertThat(bookingId).isNotBlank()
    }

    @Then("the booking should fail with reason {string}")
    fun the_booking_should_fail_with_reason(expectedReason: String) {
        assertThat(bookingResult).isInstanceOf(BookingResult.Failure::class.java)
        val actualReason = (bookingResult as BookingResult.Failure).reason
        assertThat(actualReason).isEqualTo(expectedReason)
    }

    @Then("seat {string} should still be booked by {string}")
    fun seat_should_still_be_booked_by(seatCode: String, customerName: String) {
        val (row, number) = parseSeatCode(seatCode)
        val seat = Seat(row, number, SeatSection.STANDARD)

        val booking = bookingRepository.findBySeat(seat)
        assertThat(booking).isNotNull
        assertThat(booking!!.customer.value).isEqualTo("customer-$customerName")
    }

    // Helper: Parse "A5" into row "A" and number 5
    private fun parseSeatCode(seatCode: String): Pair<String, Int> {
        val row = seatCode.substring(0, 1)
        val number = seatCode.substring(1).toInt()
        return row to number
    }
}
```

**File**: `src/test/kotlin/com/cinelux/booking/acceptance/steps/ConfirmBookingSteps.kt`

```kotlin
package com.cinelux.booking.acceptance.steps

import com.cinelux.booking.domain.model.*
import io.cucumber.java.en.*
import org.assertj.core.api.Assertions.*
import java.time.Instant

class ConfirmBookingSteps {

    private lateinit var booking: Booking
    private var confirmationError: Exception? = null

    @Given("customer {string} has booked seat {string} for {string} at {string}")
    fun customer_has_booked_seat(customer: String, seat: String, movie: String, time: String) {
        booking = Booking.create(
            id = BookingId("booking-123"),
            customer = CustomerId("customer-$customer"),
            seat = parseSeat(seat),
            showTime = createShowTime(movie, time)
        )
    }

    @Given("the booking status is {string}")
    fun the_booking_status_is(status: String) {
        when (BookingStatus.valueOf(status)) {
            BookingStatus.PENDING -> { /* Already pending */ }
            BookingStatus.CONFIRMED -> booking = booking.confirm()
            BookingStatus.CANCELLED -> booking = booking.cancel()
        }
    }

    @Given("the booking is already {string}")
    fun the_booking_is_already(status: String) {
        the_booking_status_is(status)
    }

    @Given("the booking was {string}")
    fun the_booking_was(status: String) {
        the_booking_status_is(status)
    }

    @When("the payment is successfully processed")
    fun the_payment_is_successfully_processed() {
        // Payment processing would be in Payment context
        // Here we just simulate success
    }

    @When("the booking is confirmed")
    fun the_booking_is_confirmed() {
        try {
            booking = booking.confirm()
            confirmationError = null
        } catch (e: Exception) {
            confirmationError = e
        }
    }

    @When("I attempt to confirm the booking again")
    fun i_attempt_to_confirm_again() {
        the_booking_is_confirmed()
    }

    @When("I attempt to confirm the booking")
    fun i_attempt_to_confirm() {
        the_booking_is_confirmed()
    }

    @Then("the booking status should be {string}")
    fun the_booking_status_should_be(expectedStatus: String) {
        assertThat(booking.status).isEqualTo(BookingStatus.valueOf(expectedStatus))
    }

    @Then("the confirmation timestamp should be recorded")
    fun the_confirmation_timestamp_should_be_recorded() {
        assertThat(booking.confirmedAt).isNotNull()
        assertThat(booking.confirmedAt).isBefore(Instant.now().plusSeconds(1))
    }

    @Then("the confirmation should fail with message {string}")
    fun the_confirmation_should_fail_with_message(expectedMessage: String) {
        assertThat(confirmationError).isNotNull
        assertThat(confirmationError!!.message).contains(expectedMessage)
    }

    @Then("the customer should receive a confirmation notification")
    fun customer_should_receive_notification() {
        // Would be tested via Notification context
        // Here we just document the expectation
    }

    private fun parseSeat(seatCode: String): Seat {
        val row = seatCode.substring(0, 1)
        val number = seatCode.substring(1).toInt()
        return Seat(row, number, SeatSection.STANDARD)
    }

    private fun createShowTime(movie: String, time: String) = ShowTimeReference(
        id = ShowTimeId("show-$movie"),
        movieTitle = movie,
        startTime = Instant.now().plusSeconds(3600),
        hallId = "Hall 1"
    )
}
```

---

## Fake Repositories for Testing

Create in-memory fakes instead of mocks for cleaner tests.

**File**: `src/test/kotlin/com/cinelux/booking/acceptance/fake/FakeBookingRepository.kt`

```kotlin
package com.cinelux.booking.acceptance.fake

import com.cinelux.booking.application.port.output.BookingRepository
import com.cinelux.booking.domain.model.*

class FakeBookingRepository : BookingRepository {

    private val bookings = mutableMapOf<BookingId, Booking>()

    override fun save(booking: Booking): Booking {
        bookings[booking.id] = booking
        return booking
    }

    override fun findById(id: BookingId): Booking? {
        return bookings[id]
    }

    override fun findByCustomer(customerId: CustomerId): List<Booking> {
        return bookings.values.filter { it.customer == customerId }
    }

    override fun existsConfirmedBooking(seat: Seat, showTimeId: ShowTimeId): Boolean {
        return bookings.values.any {
            it.seat == seat &&
            it.showTime.id == showTimeId &&
            it.status == BookingStatus.CONFIRMED
        }
    }

    override fun countPendingByCustomer(customerId: CustomerId): Int {
        return bookings.values.count {
            it.customer == customerId && it.status == BookingStatus.PENDING
        }
    }

    fun findBySeat(seat: Seat): Booking? {
        return bookings.values.find { it.seat == seat }
    }

    fun clear() {
        bookings.clear()
    }
}
```

---

## Test Runner Configuration

**File**: `src/test/kotlin/com/cinelux/booking/acceptance/CucumberAcceptanceTest.kt`

```kotlin
package com.cinelux.booking.acceptance

import org.junit.platform.suite.api.*

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:target/cucumber-reports/acceptance.html")
@ConfigurationParameter(key = "cucumber.glue", value = "com.cinelux.booking.acceptance.steps")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @wip")
class CucumberAcceptanceTest
```

**File**: `src/test/resources/cucumber.properties`

```properties
cucumber.publish.enabled=false
cucumber.plugin=pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json
cucumber.glue=com.cinelux.booking.acceptance.steps
cucumber.features=src/test/resources/features
```

---

## E2E Tests with Spring Boot

For full integration tests with real database and REST API.

**File**: `src/test/kotlin/com/cinelux/booking/e2e/CucumberE2ETest.kt`

```kotlin
package com.cinelux.booking.e2e

import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CucumberE2EConfig
```

**File**: `src/test/kotlin/com/cinelux/booking/e2e/steps/BookingApiSteps.kt`

```kotlin
package com.cinelux.booking.e2e.steps

import io.cucumber.java.en.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.assertj.core.api.Assertions.*

class BookingApiSteps {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private var lastResponse: ResponseEntity<Map<String, Any>>? = null

    @When("I POST to {string} with:")
    fun i_post_to_endpoint(endpoint: String, requestBody: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val request = HttpEntity(requestBody, headers)

        lastResponse = restTemplate.postForEntity(
            endpoint,
            request,
            Map::class.java
        ) as ResponseEntity<Map<String, Any>>
    }

    @Then("the response status should be {int}")
    fun the_response_status_should_be(expectedStatus: Int) {
        assertThat(lastResponse?.statusCodeValue).isEqualTo(expectedStatus)
    }

    @Then("the response should contain a booking ID")
    fun the_response_should_contain_booking_id() {
        val body = lastResponse?.body
        assertThat(body).containsKey("bookingId")
        assertThat(body!!["bookingId"]).isNotNull()
    }
}
```

---

## Running Tests

### Run All BDD Tests
```bash
mvn test
```

### Run Specific Feature
```bash
mvn test -Dcucumber.features="src/test/resources/features/booking/book-seat.feature"
```

### Run by Tag
```gherkin
@critical
Scenario: Book VIP seat
  ...
```

```bash
mvn test -Dcucumber.filter.tags="@critical"
```

### Generate HTML Reports
```bash
mvn test
# Reports in: target/cucumber-reports/cucumber.html
```

---

## Tagging Strategy

Use tags to organize and filter scenarios:

```gherkin
@booking @critical @acceptance
Feature: Book a Cinema Seat
  ...

  @happy-path
  Scenario: Successfully book an available seat
    ...

  @error-handling
  Scenario: Cannot book an already booked seat
    ...

  @wip
  Scenario: Book with discount code
    # Work in progress - skip in CI
    ...
```

**Common tags**:
- `@critical` - Core business scenarios
- `@acceptance` - Acceptance test level
- `@e2e` - Full integration tests
- `@wip` - Work in progress (skip in CI)
- `@manual` - Manual test scenarios
- `@slow` - Tests that take >5 seconds

---

## Best Practices

### 1. Use Domain Language

```gherkin
# ✅ GOOD - Domain language
Given customer "Alice" has booked seat "A5"
When the booking is confirmed

# ❌ BAD - Technical language
Given there is a booking record in the database
When the status field is updated to 2
```

### 2. Describe Behavior, Not Implementation

```gherkin
# ✅ GOOD - Behavior
When I book seat "A5"
Then the seat should be reserved

# ❌ BAD - Implementation
When I call POST /api/bookings with JSON payload
Then the database should have a row with status=PENDING
```

### 3. Keep Scenarios Focused

```gherkin
# ✅ GOOD - One behavior
Scenario: Cancel pending booking
  Given I have a pending booking
  When I cancel it
  Then it should be cancelled

# ❌ BAD - Multiple behaviors
Scenario: Booking lifecycle
  When I book a seat
  And I pay for it
  And I cancel it
  And I rebook it
  Then ...  # Too much!
```

### 4. Use Background for Common Setup

```gherkin
Feature: Booking Management

  Background:
    Given the cinema has these showtimes:
      | Movie      | Time  | Hall   |
      | The Matrix | 19:00 | Hall 1 |
    And I am logged in as "Alice"

  Scenario: Book seat
    When I book seat "A5"
    ...

  Scenario: Cancel booking
    When I cancel my booking
    ...
```

### 5. Use Scenario Outline for Data-Driven Tests

```gherkin
Scenario Outline: Book seats in different sections
  When I book seat "<seat>" in section "<section>"
  Then the price should be <price> EUR

  Examples:
    | seat | section  | price |
    | A5   | STANDARD | 10.00 |
    | C4   | VIP      | 15.00 |
    | D2   | PREMIUM  | 20.00 |
```

---

## Domain Unit Tests (Still Important!)

BDD doesn't replace unit tests for domain logic. Keep fast unit tests for:

```kotlin
// src/test/kotlin/com/cinelux/booking/domain/model/BookingTest.kt
class BookingTest {

    @Test
    fun `should confirm pending booking`() {
        val booking = createTestBooking(status = BookingStatus.PENDING)

        val confirmed = booking.confirm()

        assertEquals(BookingStatus.CONFIRMED, confirmed.status)
        assertNotNull(confirmed.confirmedAt)
    }

    @Test
    fun `should throw exception when confirming already confirmed booking`() {
        val booking = createTestBooking(status = BookingStatus.CONFIRMED)

        val exception = assertThrows<IllegalArgumentException> {
            booking.confirm()
        }

        assertTrue(exception.message!!.contains("Only pending bookings"))
    }
}
```

**Why keep unit tests:**
- Fast feedback (milliseconds)
- Pinpoint failures precisely
- Test edge cases exhaustively
- No framework overhead

---

## CI/CD Pipeline

```bash
# Fast feedback - unit tests only
mvn test -Dtest="**/domain/**/*Test"

# Acceptance tests
mvn test -Dcucumber.filter.tags="@acceptance and not @slow"

# Full suite including E2E
mvn test -Dcucumber.filter.tags="not @manual and not @wip"
```

---

## Coverage and Reporting

### Cucumber Reports

After running tests:
```bash
open target/cucumber-reports/cucumber.html
```

### Living Documentation

Feature files serve as documentation that:
- ✅ Is always up-to-date (tests fail if outdated)
- ✅ Is readable by non-developers
- ✅ Can be reviewed by domain experts
- ✅ Captures business rules explicitly

---

## Next Steps

1. **Add Cucumber dependencies** to `pom.xml`
2. **Create first feature file**: `book-seat.feature`
3. **Implement step definitions**: `BookingSteps.kt`
4. **Run tests**: `mvn test`
5. **Share with domain experts** to validate scenarios
6. **Iterate**: Add scenarios as you discover new requirements

This BDD approach will give you both executable tests AND living documentation that evolves with your system!
