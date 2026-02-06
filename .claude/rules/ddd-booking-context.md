---
paths: src/main/kotlin/com/cinelux/booking/**/*.kt
---

# Domain-Driven Design - Booking Context

## Bounded Context: Booking

The **Booking Context** handles the reservation of cinema seats for specific showtimes. This is the core business capability of CineLux.

### Context Boundaries

**Within this context:**
- Managing seat reservations
- Booking lifecycle (pending → confirmed → cancelled)
- Seat availability validation
- Customer booking history

**Outside this context** (handled by other contexts later):
- Payment processing (Payment Context)
- Movie catalog (Catalog Context)
- User authentication (Customer Context)
- Scheduling movies (Screening Context)

---

## Ubiquitous Language

These terms MUST be used consistently across code, conversations, and documentation:

| Term | Definition | Type |
|------|------------|------|
| **Booking** | A reservation of a specific seat for a showtime | Aggregate Root |
| **Seat** | A physical location in a cinema hall (row + number) | Value Object |
| **ShowTime** | A scheduled screening of a movie at a specific time | Entity Reference |
| **Customer** | Person making the booking | Entity Reference |
| **BookingStatus** | State of a booking (Pending, Confirmed, Cancelled) | Enum |
| **SeatSection** | Category of seat (Standard, VIP, Premium) | Enum |
| **HallLayout** | Configuration of seats in a cinema hall | Entity |

**Forbidden terms in code:**
- ❌ "Reservation" (use Booking)
- ❌ "Ticket" (too vague - that's a printed artifact)
- ❌ "Order" (that's payment domain)
- ❌ "User" (use Customer in this context)

---

## Domain Model

### Aggregate: Booking

**Booking** is the only aggregate root in this context. It ensures consistency for all booking operations.

```kotlin
package com.cinelux.booking.domain.model

import java.time.Instant

data class Booking(
    val id: BookingId,
    val customer: CustomerId,
    val seat: Seat,
    val showTime: ShowTimeReference,
    val status: BookingStatus,
    val createdAt: Instant,
    val confirmedAt: Instant? = null,
    val cancelledAt: Instant? = null
) {
    // Aggregate root encapsulates business rules

    fun confirm(at: Instant = Instant.now()): Booking {
        require(status == BookingStatus.PENDING) {
            "Only pending bookings can be confirmed. Current status: $status"
        }
        return copy(
            status = BookingStatus.CONFIRMED,
            confirmedAt = at
        )
    }

    fun cancel(at: Instant = Instant.now()): Booking {
        require(status != BookingStatus.CANCELLED) {
            "Booking is already cancelled"
        }
        return copy(
            status = BookingStatus.CANCELLED,
            cancelledAt = at
        )
    }

    fun isPending(): Boolean = status == BookingStatus.PENDING
    fun isConfirmed(): Boolean = status == BookingStatus.CONFIRMED
    fun isCancelled(): Boolean = status == BookingStatus.CANCELLED

    companion object {
        fun create(
            id: BookingId,
            customer: CustomerId,
            seat: Seat,
            showTime: ShowTimeReference
        ): Booking {
            return Booking(
                id = id,
                customer = customer,
                seat = seat,
                showTime = showTime,
                status = BookingStatus.PENDING,
                createdAt = Instant.now()
            )
        }
    }
}
```

**Why Booking is an Aggregate:**
1. **Consistency boundary**: Seat availability for a showtime must be consistent
2. **Transactional boundary**: All booking changes happen atomically
3. **Invariant enforcement**: Only aggregate controls status transitions

---

### Value Objects

#### Seat (Core Value Object)

```kotlin
package com.cinelux.booking.domain.model

data class Seat(
    val row: String,
    val number: Int,
    val section: SeatSection
) {
    init {
        require(row.matches(Regex("^[A-Z]$"))) {
            "Row must be a single uppercase letter (A-Z), got: $row"
        }
        require(number in 1..50) {
            "Seat number must be between 1 and 50, got: $number"
        }
    }

    fun displayName(): String = "$row$number"

    override fun toString(): String = displayName()
}

enum class SeatSection {
    STANDARD,
    VIP,
    PREMIUM;

    fun priceMultiplier(): Double = when (this) {
        STANDARD -> 1.0
        VIP -> 1.5
        PREMIUM -> 2.0
    }
}
```

**Why Seat is a Value Object:**
- No identity (two seats with same row/number/section are identical)
- Immutable
- Side-effect free methods
- Self-validating

#### Identity Value Objects

```kotlin
package com.cinelux.booking.domain.model

// Use inline value classes for type safety + performance
@JvmInline
value class BookingId(val value: String) {
    init {
        require(value.isNotBlank()) { "BookingId cannot be blank" }
    }
}

@JvmInline
value class CustomerId(val value: String) {
    init {
        require(value.isNotBlank()) { "CustomerId cannot be blank" }
    }
}

@JvmInline
value class ShowTimeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ShowTimeId cannot be blank" }
    }
}
```

**Why use inline value classes:**
- Type safety (can't mix up BookingId and CustomerId)
- Zero runtime overhead (compiled to underlying String)
- Clear domain intent

---

### Enumerations

```kotlin
package com.cinelux.booking.domain.model

enum class BookingStatus {
    PENDING,      // Just created, awaiting confirmation
    CONFIRMED,    // Payment successful, seat reserved
    CANCELLED;    // Cancelled by user or system

    fun canTransitionTo(newStatus: BookingStatus): Boolean = when (this) {
        PENDING -> newStatus in setOf(CONFIRMED, CANCELLED)
        CONFIRMED -> newStatus == CANCELLED
        CANCELLED -> false  // Terminal state
    }
}
```

---

## Domain Services

Use domain services when:
- Logic involves multiple aggregates
- Operation doesn't naturally belong to one entity
- Cross-aggregate business rules

```kotlin
package com.cinelux.booking.domain.service

import com.cinelux.booking.domain.model.*

interface SeatAvailabilityChecker {
    fun isAvailable(seat: Seat, showTime: ShowTimeReference): Boolean
}

class SeatAvailabilityCheckerImpl(
    private val bookingRepository: BookingRepository  // Port, not infrastructure!
) : SeatAvailabilityChecker {

    override fun isAvailable(seat: Seat, showTime: ShowTimeReference): Boolean {
        // Business rule: Seat is available if no confirmed booking exists
        return !bookingRepository.existsConfirmedBooking(seat, showTime.id)
    }
}
```

**Note**: Domain services are in `domain/service/`, not `application/`. They encapsulate pure business logic.

---

## Domain Events

Events communicate state changes to other contexts or internal listeners.

```kotlin
package com.cinelux.booking.domain.event

import com.cinelux.booking.domain.model.*
import java.time.Instant

sealed interface BookingDomainEvent {
    val occurredAt: Instant
    val bookingId: BookingId
}

data class BookingCreated(
    override val bookingId: BookingId,
    val customerId: CustomerId,
    val seat: Seat,
    val showTime: ShowTimeReference,
    override val occurredAt: Instant = Instant.now()
) : BookingDomainEvent

data class BookingConfirmed(
    override val bookingId: BookingId,
    override val occurredAt: Instant = Instant.now()
) : BookingDomainEvent

data class BookingCancelled(
    override val bookingId: BookingId,
    val reason: String?,
    override val occurredAt: Instant = Instant.now()
) : BookingDomainEvent
```

**Usage:**
- Payment Context listens to `BookingCreated` to initiate payment
- Notification Context listens to `BookingConfirmed` to send email
- Analytics Context listens to all events for reporting

---

## Domain Exceptions

Custom exceptions for domain-specific failures.

```kotlin
package com.cinelux.booking.domain.exception

import com.cinelux.booking.domain.model.*

sealed class BookingDomainException(message: String) : RuntimeException(message)

class SeatNotAvailableException(
    val seat: Seat,
    val showTime: ShowTimeReference
) : BookingDomainException(
    "Seat ${seat.displayName()} is not available for ${showTime.movieTitle} at ${showTime.startTime}"
)

class InvalidBookingStateException(
    val bookingId: BookingId,
    val currentState: BookingStatus,
    val attemptedAction: String
) : BookingDomainException(
    "Cannot $attemptedAction booking $bookingId in state $currentState"
)

class BookingNotFoundException(
    val bookingId: BookingId
) : BookingDomainException("Booking not found: $bookingId")
```

---

## Invariants

Rules that MUST always be true in the Booking Context:

1. **Seat Uniqueness**: One confirmed booking per seat per showtime
   ```kotlin
   // Enforced by: SeatAvailabilityChecker
   // Verified by: BookingRepository constraint
   ```

2. **Status Transition**: Valid state machine transitions only
   ```kotlin
   // Enforced by: Booking.confirm(), Booking.cancel()
   // Verified by: BookingStatus.canTransitionTo()
   ```

3. **Temporal Validity**: Cannot book seats for past showtimes
   ```kotlin
   fun BookingService.validateShowTime(showTime: ShowTimeReference) {
       require(showTime.startTime.isAfter(Instant.now())) {
           "Cannot book seats for past showtimes"
       }
   }
   ```

4. **Customer Limit**: Customer can have max 10 pending bookings
   ```kotlin
   // Enforced in: BookSeatUseCase
   // Policy decision, not hard domain rule
   ```

---

## Repository Contracts (SPI Ports)

Repositories are **SPI ports** (Service Provider Interface) defined in `application/port/spi/`.

```kotlin
package com.cinelux.booking.application.port.spi

interface BookingRepository {
    fun save(booking: Booking): Booking
    fun findById(id: BookingId): Booking?
    fun findByCustomer(customerId: CustomerId): List<Booking>
    fun existsConfirmedBooking(seat: Seat, showTimeId: ShowTimeId): Boolean
    fun countPendingByCustomer(customerId: CustomerId): Int
}
```

**Why these methods:**
- Each method serves a specific use case
- No generic "findAll()" - be explicit about queries
- Domain language in method names

---

## Testing Domain Model

Domain tests should be pure - no Spring, no database, no mocks.

```kotlin
package com.cinelux.booking.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BookingTest {

    @Test
    fun `should confirm pending booking`() {
        val booking = createTestBooking(status = BookingStatus.PENDING)

        val confirmed = booking.confirm()

        assertEquals(BookingStatus.CONFIRMED, confirmed.status)
        assertNotNull(confirmed.confirmedAt)
    }

    @Test
    fun `should not confirm already confirmed booking`() {
        val booking = createTestBooking(status = BookingStatus.CONFIRMED)

        val exception = assertThrows<IllegalArgumentException> {
            booking.confirm()
        }

        assertEquals("Only pending bookings can be confirmed", exception.message)
    }

    private fun createTestBooking(status: BookingStatus = BookingStatus.PENDING) = Booking(
        id = BookingId("test-id"),
        customer = CustomerId("customer-1"),
        seat = Seat("A", 1, SeatSection.STANDARD),
        showTime = ShowTimeReference(
            id = ShowTimeId("show-1"),
            movieTitle = "Test Movie",
            startTime = Instant.now().plusSeconds(3600),
            hallId = "hall-1"
        ),
        status = status,
        createdAt = Instant.now()
    )
}
```
