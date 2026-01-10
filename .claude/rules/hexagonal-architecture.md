---
paths: src/main/kotlin/**/*.kt
---

# Hexagonal Architecture Rules

## Overview

Hexagonal Architecture (Ports & Adapters) isolates business logic from external concerns. The domain is at the center, surrounded by ports (interfaces), and infrastructure implementations.

**Terminology**:
- **API ports** (`application/port/api/`) = Driving ports (what application exposes) - formerly "input ports"
- **SPI ports** (`application/port/spi/`) = Driven ports (what application needs) - formerly "output ports"
- **Infrastructure** (`infrastructure/`) = Framework implementations - formerly "adapters"

## Layer Definitions

### Domain Layer (`domain/`)

**Purpose**: Pure business logic - the heart of your application

**Allowed dependencies**:
- ✅ Kotlin stdlib only
- ✅ Other domain classes within same bounded context
- ✅ Domain exceptions

**Forbidden dependencies**:
- ❌ Spring annotations (`@Component`, `@Service`, `@Autowired`)
- ❌ JPA annotations (`@Entity`, `@Table`, `@Id`)
- ❌ Jackson annotations (`@JsonProperty`)
- ❌ Any infrastructure or port packages
- ❌ Javax/Jakarta validation (`@Valid`, `@NotNull`)

**Example: Correct Domain Entity**
```kotlin
package com.cinelux.booking.domain.model

data class Booking(
    val id: BookingId,
    val customer: CustomerId,
    val seat: Seat,
    val showTime: ShowTime,
    val status: BookingStatus,
    val createdAt: Instant
) {
    fun confirm(): Booking {
        require(status == BookingStatus.PENDING) {
            "Can only confirm pending bookings"
        }
        return copy(status = BookingStatus.CONFIRMED)
    }

    fun cancel(): Booking {
        require(status != BookingStatus.CANCELLED) {
            "Booking already cancelled"
        }
        return copy(status = BookingStatus.CANCELLED)
    }
}

// Value Objects
@JvmInline
value class BookingId(val value: String)

@JvmInline
value class CustomerId(val value: String)

data class Seat(
    val row: String,
    val number: Int,
    val section: SeatSection
) {
    init {
        require(row.matches(Regex("[A-Z]"))) { "Row must be A-Z" }
        require(number > 0) { "Seat number must be positive" }
    }
}

enum class BookingStatus {
    PENDING, CONFIRMED, CANCELLED
}

enum class SeatSection {
    STANDARD, VIP, PREMIUM
}
```

**Example: WRONG - Domain with Framework Dependencies**
```kotlin
// ❌ BAD - Spring annotations in domain
@Entity
@Table(name = "bookings")
data class Booking(
    @Id val id: BookingId,
    val customer: CustomerId
)

// ❌ BAD - Jackson annotation
data class Booking(
    @JsonProperty("booking_id") val id: BookingId
)
```

---

### Application Layer (`application/`)

**Purpose**: Orchestrate domain objects and coordinate workflows (use cases)

**Purpose**: Orchestrate domain objects and coordinate workflows (use cases)

#### API Ports (`application/port/api/`)

Interfaces defining what the application **can do** (driving side - Application Programming Interface).

**Example: Use Case API Port**
```kotlin
package com.cinelux.booking.application.port.api

interface BookSeatUseCase {
    fun execute(command: BookSeatCommand): BookingResult
}

data class BookSeatCommand(
    val customerId: String,
    val showTimeId: String,
    val row: String,
    val seatNumber: Int
)

sealed interface BookingResult {
    data class Success(val bookingId: String) : BookingResult
    data class Failure(val reason: String) : BookingResult
}
```

#### SPI Ports (`application/port/spi/`)

Interfaces defining what the application **needs** (driven side - Service Provider Interface).

**Example: Repository SPI Port**
```kotlin
package com.cinelux.booking.application.port.spi

interface BookingRepository {
    fun save(booking: Booking): Booking
    fun findById(id: BookingId): Booking?
    fun existsBySeatAndShowTime(seat: Seat, showTime: ShowTime): Boolean
}

interface ShowTimeRepository {
    fun findById(id: ShowTimeId): ShowTime?
}
```

#### Use Case Implementations (`application/usecase/`)

**Rules**:
- ✅ Can depend on: Domain, API Ports, SPI Ports
- ❌ Cannot depend on: Infrastructure, frameworks

**Example: Use Case Implementation**
```kotlin
package com.cinelux.booking.application.usecase

import com.cinelux.booking.application.port.api.*
import com.cinelux.booking.application.port.spi.*
import com.cinelux.booking.domain.model.*
import java.time.Instant

class BookSeatUseCaseImpl(
    private val bookingRepository: BookingRepository,
    private val showTimeRepository: ShowTimeRepository
) : BookSeatUseCase {

    override fun execute(command: BookSeatCommand): BookingResult {
        // Validate show time exists
        val showTime = showTimeRepository.findById(ShowTimeId(command.showTimeId))
            ?: return BookingResult.Failure("Show time not found")

        // Create value objects
        val seat = Seat(
            row = command.row,
            number = command.seatNumber,
            section = SeatSection.STANDARD
        )

        // Check availability (business rule)
        if (bookingRepository.existsBySeatAndShowTime(seat, showTime)) {
            return BookingResult.Failure("Seat already booked")
        }

        // Create domain entity
        val booking = Booking(
            id = BookingId(generateId()),
            customer = CustomerId(command.customerId),
            seat = seat,
            showTime = showTime,
            status = BookingStatus.PENDING,
            createdAt = Instant.now()
        )

        // Persist via port
        val savedBooking = bookingRepository.save(booking)

        return BookingResult.Success(savedBooking.id.value)
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}
```

---

### Infrastructure Layer (`infrastructure/`)

**Purpose**: Implement ports using frameworks and external libraries

#### API Infrastructure (`infrastructure/api/rest/`)

Translate HTTP requests to use case commands.

**Rules**:
- ✅ Spring annotations allowed here
- ✅ Depends on: API Ports, DTOs
- ❌ Never depends on: Domain directly, SPI Ports

**Example: REST Controller**
```kotlin
package com.cinelux.booking.infrastructure.api.rest

import com.cinelux.booking.application.port.api.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookSeatUseCase: BookSeatUseCase
) {

    @PostMapping
    fun bookSeat(@RequestBody request: BookSeatRequest): ResponseEntity<BookSeatResponse> {
        val command = BookSeatCommand(
            customerId = request.customerId,
            showTimeId = request.showTimeId,
            row = request.row,
            seatNumber = request.seatNumber
        )

        return when (val result = bookSeatUseCase.execute(command)) {
            is BookingResult.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BookSeatResponse(bookingId = result.bookingId))

            is BookingResult.Failure -> ResponseEntity
                .badRequest()
                .body(BookSeatResponse(error = result.reason))
        }
    }
}

// DTOs for REST API
data class BookSeatRequest(
    val customerId: String,
    val showTimeId: String,
    val row: String,
    val seatNumber: Int
)

data class BookSeatResponse(
    val bookingId: String? = null,
    val error: String? = null
)
```

#### SPI Infrastructure (`infrastructure/persistence/`)

Implement repository ports using JPA/JDBC.

**Rules**:
- ✅ JPA/Spring Data annotations allowed
- ✅ Create separate JPA entities (don't annotate domain entities!)
- ✅ Map between domain entities and JPA entities

**Example: JPA Implementation**
```kotlin
package com.cinelux.booking.infrastructure.persistence

import com.cinelux.booking.application.port.spi.BookingRepository
import com.cinelux.booking.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import jakarta.persistence.*
import java.time.Instant

// JPA Entity (adapter concern)
@Entity
@Table(name = "bookings")
data class BookingJpaEntity(
    @Id
    val id: String,

    @Column(nullable = false)
    val customerId: String,

    @Column(nullable = false)
    val seatRow: String,

    @Column(nullable = false)
    val seatNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val seatSection: SeatSection,

    @Column(nullable = false)
    val showTimeId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: BookingStatus,

    @Column(nullable = false)
    val createdAt: Instant
)

// Spring Data Repository
interface BookingJpaRepository : JpaRepository<BookingJpaEntity, String> {
    fun existsBySeatRowAndSeatNumberAndShowTimeId(row: String, number: Int, showTimeId: String): Boolean
}

// Infrastructure implementing SPI port
@Repository
class BookingRepositoryImpl(
    private val jpaRepository: BookingJpaRepository
) : BookingRepository {

    override fun save(booking: Booking): Booking {
        val entity = booking.toJpaEntity()
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: BookingId): Booking? {
        return jpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun existsBySeatAndShowTime(seat: Seat, showTime: ShowTime): Boolean {
        return jpaRepository.existsBySeatRowAndSeatNumberAndShowTimeId(
            row = seat.row,
            number = seat.number,
            showTimeId = showTime.id.value
        )
    }

    // Mappers
    private fun Booking.toJpaEntity() = BookingJpaEntity(
        id = id.value,
        customerId = customer.value,
        seatRow = seat.row,
        seatNumber = seat.number,
        seatSection = seat.section,
        showTimeId = showTime.id.value,
        status = status,
        createdAt = createdAt
    )

    private fun BookingJpaEntity.toDomain() = Booking(
        id = BookingId(id),
        customer = CustomerId(customerId),
        seat = Seat(seatRow, seatNumber, seatSection),
        showTime = ShowTime(ShowTimeId(showTimeId)), // Simplified
        status = status,
        createdAt = createdAt
    )
}
```

---

## Configuration (`infrastructure/config/`)

Wire use cases with their dependencies.

**Example: Spring Configuration**
```kotlin
package com.cinelux.booking.infrastructure.config

import com.cinelux.booking.application.port.api.BookSeatUseCase
import com.cinelux.booking.application.port.spi.*
import com.cinelux.booking.application.usecase.BookSeatUseCaseImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BookingConfig {

    @Bean
    fun bookSeatUseCase(
        bookingRepository: BookingRepository,
        showTimeRepository: ShowTimeRepository
    ): BookSeatUseCase {
        return BookSeatUseCaseImpl(bookingRepository, showTimeRepository)
    }
}
```

---

## Architecture Validation Checklist

Before committing code, verify:

1. **Domain purity**:
   ```bash
   grep -r "@\(Component\|Entity\|RestController\|Service\)" src/main/kotlin/com/cinelux/*/domain/
   # Must return NOTHING
   ```

2. **Dependency direction**:
   ```bash
   grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/domain/
   grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/application/
   # Must return NOTHING
   ```

3. **Port isolation**:
   - Ports should only reference domain types
   - No framework types in port signatures

4. **Infrastructure responsibilities**:
   - Each infrastructure class = one port implementation
   - Infrastructure contains all framework code
   - Mapping between framework types and domain types

---

## Common Mistakes to Avoid

### ❌ Annotating Domain Entities with JPA
```kotlin
// WRONG
@Entity
data class Booking(...)
```

### ❌ Using Domain Entities in REST Responses
```kotlin
// WRONG
@GetMapping("/{id}")
fun getBooking(@PathVariable id: String): Booking
```

### ❌ Mixing Port and Infrastructure Code
```kotlin
// WRONG - Repository interface with Spring annotation
@Repository
interface BookingRepository { ... }
```

### ✅ Correct Approach
- Separate JPA entities from domain entities
- Use DTOs for REST API
- Ports are plain interfaces with no annotations
- Infrastructure implements ports and use framework code
