---
paths: src/main/kotlin/com/cinelux/screening/**/*.kt
---

# Domain-Driven Design - Screening Context

## Bounded Context: Screening

The **Screening Context** manages movie scheduling — which movies play in which halls and when. It is the upstream authority on showtimes.

### Context Boundaries

**Within this context:**
- Managing showtimes (movie, hall, start time)
- Hall definitions (id, name)
- Querying showtimes by day of week

**Outside this context** (handled by other contexts):
- Seat reservations (Booking Context)
- Movie catalog details (Catalog Context — future)
- Payment processing (Payment Context — future)

---

## Ubiquitous Language

These terms MUST be used consistently across code, conversations, and documentation:

| Term | Definition | Type |
|------|------------|------|
| **ShowTime** | A scheduled screening of a movie at a specific time in a specific hall | Aggregate Root |
| **Hall** | A physical cinema room where movies are screened | Value Object |
| **ShowTimeId** | Unique identifier for a showtime | Identity Value Object |
| **DayOfWeek** | The day a showtime occurs, derived from its start time | Derived attribute |

**Forbidden terms in code:**
- ❌ "Session" (use ShowTime)
- ❌ "Screening" as entity name (that's the context name, not an entity)
- ❌ "Room" (use Hall)
- ❌ "Schedule" as entity (too generic — use ShowTime)

---

## Domain Model

### Aggregate: ShowTime

**ShowTime** is the aggregate root of this context. It represents one screening of a movie.

```kotlin
package com.cinelux.screening.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class ShowTime(
    val id: ShowTimeId,
    val movieTitle: String,
    val hall: Hall,
    val startTime: Instant,
    val dayOfWeek: DayOfWeek
) {
    companion object {
        fun create(
            id: ShowTimeId,
            movieTitle: String,
            hall: Hall,
            startTime: Instant
        ): ShowTime {
            require(movieTitle.isNotBlank()) { "Movie title must not be blank" }
            return ShowTime(
                id = id,
                movieTitle = movieTitle,
                hall = hall,
                startTime = startTime,
                dayOfWeek = startTime.atZone(ZoneId.systemDefault()).dayOfWeek
            )
        }
    }
}
```

### Value Objects

#### Hall

```kotlin
package com.cinelux.screening.domain.model

data class Hall(
    val id: String,
    val name: String
) {
    init {
        require(id.isNotBlank()) { "Hall id must not be blank" }
        require(name.isNotBlank()) { "Hall name must not be blank" }
    }
}
```

#### ShowTimeId (Identity Value Object)

```kotlin
package com.cinelux.screening.domain.model

@JvmInline
value class ShowTimeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ShowTimeId must not be blank" }
    }
}
```

---

## Invariants

Rules that MUST always be true in the Screening Context:

1. **Movie title required**: ShowTime must have a non-blank movie title
   ```kotlin
   // Enforced by: ShowTime.create() factory method
   ```

2. **Valid hall**: Hall must have a non-blank id and name
   ```kotlin
   // Enforced by: Hall init block
   ```

3. **Day of week derived**: DayOfWeek is always computed from startTime, never set independently
   ```kotlin
   // Enforced by: ShowTime.create() — dayOfWeek is derived, not accepted as input
   ```

---

## Repository Contracts (SPI Ports)

Repositories are **SPI ports** defined in `application/port/spi/`.

```kotlin
package com.cinelux.screening.application.port.spi

interface ShowTimeRepository {
    fun findByDayOfWeek(dayOfWeek: DayOfWeek): List<ShowTime>
}
```

**Why this method:**
- Serves the `ListShowTimesUseCase` — query showtimes for a given day
- Uses domain language (`DayOfWeek`, not raw strings)

---

## Testing Domain Model

Domain tests should be pure — no Spring, no database, no mocks.

```kotlin
package com.cinelux.screening.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.Instant
import kotlin.test.assertEquals

class ShowTimeTest {

    @Test
    fun `should derive correct day of week from start time`() {
        // Given a known Monday instant
        val monday = Instant.parse("2025-01-06T20:00:00Z")

        val showTime = ShowTime.create(
            id = ShowTimeId("show-1"),
            movieTitle = "Test Movie",
            hall = Hall("hall-1", "Main Hall"),
            startTime = monday
        )

        assertEquals(DayOfWeek.MONDAY, showTime.dayOfWeek)
    }

    @Test
    fun `should reject blank movie title`() {
        assertThrows<IllegalArgumentException> {
            ShowTime.create(
                id = ShowTimeId("show-1"),
                movieTitle = "",
                hall = Hall("hall-1", "Main Hall"),
                startTime = Instant.now()
            )
        }
    }
}
```

**Acceptance tests** live in `src/test/kotlin/com/cinelux/screening/acceptance/` using Cucumber with Gherkin features in `src/test/resources/features/screening/`.
