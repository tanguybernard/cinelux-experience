---
paths: src/main/kotlin/com/cinelux/**/*.kt
---

# DDD Strategic Design - Cross-Context Rules

## Context Map

CineLux is organized into bounded contexts that communicate through well-defined boundaries.

### Current Contexts

| Context | Responsibility | Status |
|---------|---------------|--------|
| **Booking** | Seat reservation lifecycle | Active |
| **Screening** | Movie scheduling and showtimes | Active |
| **Payment** | Payment processing | Future |
| **Customer** | User authentication and profiles | Future |

### Context Relationships

```
┌─────────────┐        ShowTimeReference        ┌──────────────┐
│   Booking    │ ─────────────────────────────▶  │  Screening   │
│  (Conformist)│   ID + denormalized fields      │  (Upstream)  │
└─────────────┘                                  └──────────────┘
       │                                                │
       │  BookingCreated event                          │
       ▼                                                ▼
┌─────────────┐                                 ┌──────────────┐
│   Payment   │                                 │   Catalog    │
│   (Future)  │                                 │   (Future)   │
└─────────────┘                                 └──────────────┘
```

**Booking → Screening**: Conformist relationship. Booking references ShowTime via `ShowTimeReference` (ID + denormalized fields like movieTitle, startTime, hallId). Booking conforms to Screening's model.

**Booking → Payment**: Domain events. `BookingCreated` triggers payment flow (future).

---

## Cross-Context Rules

### Never Import Across Contexts

```kotlin
// ❌ WRONG - Direct import from another context
import com.cinelux.screening.domain.model.ShowTime

// ✅ CORRECT - Use a local reference with only the data you need
data class ShowTimeReference(
    val id: ShowTimeId,
    val movieTitle: String,
    val startTime: Instant,
    val hallId: String
)
```

### Entity References

When referencing entities from another bounded context:

1. **Use ID references** — never embed the full foreign entity
2. **Denormalize essential fields** — cache display/validation data locally
3. **Keep references minimal** — only include what this context needs
4. **Define references in your own domain** — the reference type belongs to the consuming context

### Domain Events for Async Communication

Contexts communicate state changes via domain events:

```kotlin
// Published by Booking context
sealed interface BookingDomainEvent {
    val occurredAt: Instant
    val bookingId: BookingId
}

// Consumed by Payment context (future)
// Each context defines its own event handlers
```

**Rules for domain events:**
- Events are immutable facts about something that happened
- Use past tense naming: `BookingCreated`, `BookingConfirmed`
- Events belong to the publishing context
- Consumers define their own handlers and reaction logic

---

## General DDD Patterns

### ✅ Rich Domain Model

Business logic lives **inside** domain entities and value objects:

```kotlin
// CORRECT - Business logic in entity
data class Booking(...) {
    fun confirm(): Booking {
        require(status == BookingStatus.PENDING)
        return copy(status = BookingStatus.CONFIRMED)
    }
}
```

### ❌ Anemic Domain Model (Anti-Pattern)

```kotlin
// WRONG - No business logic, just data
data class Booking(var status: BookingStatus)

// Business logic in service instead of entity
class BookingService {
    fun confirmBooking(booking: Booking) {
        booking.status = BookingStatus.CONFIRMED  // BAD!
    }
}
```

### ❌ Smart UI Anti-Pattern

```kotlin
// WRONG - Business rules in controller
@PostMapping("/confirm")
fun confirm(@PathVariable id: String) {
    val booking = repo.findById(id)
    if (booking.status == BookingStatus.PENDING) {  // Business logic in infrastructure!
        booking.status = BookingStatus.CONFIRMED
        repo.save(booking)
    }
}
```

### ✅ Correct Layering

```kotlin
// Use case handles workflow, domain handles rules
class ConfirmBookingUseCase {
    override fun execute(command: ConfirmBookingCommand) {
        val booking = repository.findById(command.bookingId)
        val confirmed = booking.confirm()  // Domain logic
        repository.save(confirmed)
    }
}
```

---

## Evolving the Domain

As you learn more about the business:

1. **Refine Ubiquitous Language**: Update terms based on domain expert feedback
2. **Split Aggregates**: If performance suffers, consider smaller aggregates
3. **Extract Bounded Contexts**: When complexity grows, identify new contexts
4. **Add Domain Events**: Make implicit processes explicit

---

## Adding a New Bounded Context

Checklist when introducing a new context:

1. **Create top-level package**: `com.cinelux.<context>/`
2. **Duplicate the layer structure**:
   ```
   <context>/
   ├── domain/model/
   ├── application/
   │   ├── usecase/
   │   └── port/
   │       ├── api/    # Driving ports
   │       └── spi/    # Driven ports
   └── infrastructure/
   ```
3. **Define ubiquitous language** in a new `.claude/rules/ddd-<context>-context.md` file
4. **Update context map** in this file with the new relationships
5. **Use domain events** for cross-context communication — never direct imports
6. **Add Cucumber test runner** for the new context's acceptance tests
7. **Add ArchUnit rules** to enforce boundary isolation
