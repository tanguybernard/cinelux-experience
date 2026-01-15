---
paths: src/**/*.kt
---

# Kotlin Coding Standards for CineLux

## General Principles

1. **Immutability by default**: Use `val` over `var` unless mutation is necessary
2. **Null safety**: Avoid nullable types when possible; use sealed classes for optional results
3. **Explicit over clever**: Prefer readable code over Kotlin magic
4. **Data classes for domain**: Use data classes for entities and value objects
5. **Functional style**: Prefer expressions over statements when clear

---

## Naming Conventions

### Classes and Interfaces

```kotlin
// ✅ PascalCase for classes
class BookSeatUseCase
data class Booking
interface BookingRepository

// ✅ Suffixes for ports and use cases
interface BookSeatUseCase  // Input port
interface BookingRepository  // Output port
class BookSeatUseCaseImpl  // Implementation

// ❌ Avoid generic names
class Manager
class Helper
class Util
```

### Functions and Variables

```kotlin
// ✅ camelCase for functions and variables
fun confirmBooking()
val bookingId = "123"
val isAvailable = checkAvailability()

// ✅ Boolean properties start with "is", "has", "can"
val isConfirmed: Boolean
val hasSeats: Boolean
val canCancel: Boolean

// ❌ Avoid Hungarian notation
val strBookingId  // Wrong
val bookingIdString  // Wrong
```

### Constants

```kotlin
// ✅ UPPER_SNAKE_CASE in companion objects
companion object {
    const val MAX_BOOKINGS_PER_CUSTOMER = 10
    const val BOOKING_EXPIRATION_MINUTES = 15
}
```

### Packages

```kotlin
// ✅ All lowercase, no underscores
package com.cinelux.booking.domain.model
package com.cinelux.booking.infrastructure.api.rest

// ❌ Avoid
package com.cinelux.Booking
package com.cinelux.booking_domain
```

---

## Type Safety

### Use Value Classes for Identifiers

```kotlin
// ✅ Type-safe IDs with zero overhead
@JvmInline
value class BookingId(val value: String)

@JvmInline
value class CustomerId(val value: String)

// ✅ Benefits: Can't accidentally mix IDs
fun findBooking(id: BookingId): Booking  // Compile-time safety

// ❌ Avoid stringly-typed IDs
fun findBooking(id: String): Booking  // Can pass any string!
```

### Use Enums for Fixed Sets

```kotlin
// ✅ Type-safe states
enum class BookingStatus {
    PENDING, CONFIRMED, CANCELLED
}

// ✅ With behavior
enum class SeatSection {
    STANDARD, VIP, PREMIUM;

    fun priceMultiplier() = when (this) {
        STANDARD -> 1.0
        VIP -> 1.5
        PREMIUM -> 2.0
    }
}

// ❌ Avoid magic strings
val status = "pending"  // Typo-prone!
```

### Use Sealed Classes for Variants

```kotlin
// ✅ Sealed classes for result types
sealed interface BookingResult {
    data class Success(val bookingId: BookingId) : BookingResult
    data class Failure(val reason: String) : BookingResult
}

// ✅ Exhaustive when expressions
when (result) {
    is BookingResult.Success -> handleSuccess(result.bookingId)
    is BookingResult.Failure -> handleError(result.reason)
}  // Compiler ensures all cases handled

// ❌ Avoid nullable returns for business failures
fun bookSeat(): Booking?  // Why null? Error? Not found? Business rule?
```

---

## Null Safety

### Prefer Non-Nullable Types

```kotlin
// ✅ Non-null when possible
data class Booking(
    val id: BookingId,
    val customer: CustomerId  // Always present
)

// ✅ Use sealed classes instead of null
sealed interface BookingSearchResult {
    data class Found(val booking: Booking) : BookingSearchResult
    data object NotFound : BookingSearchResult
}

// ❌ Avoid nullable for domain logic
data class Booking(
    val customer: CustomerId?  // When would this be null?
)
```

### Safe Null Handling

```kotlin
// ✅ Use safe call and elvis operator
val customerName = booking?.customer?.name ?: "Unknown"

// ✅ Use let for null-safe transformations
booking?.let { processBooking(it) }

// ❌ Avoid !! (not-null assertion)
val booking = findBooking(id)!!  // Will crash if null!

// ✅ Use requireNotNull with message
val booking = requireNotNull(findBooking(id)) {
    "Booking $id must exist at this point"
}
```

---

## Data Classes

### Domain Entities and Value Objects

```kotlin
// ✅ Data classes for domain objects
data class Booking(
    val id: BookingId,
    val customer: CustomerId,
    val seat: Seat,
    val status: BookingStatus,
    val createdAt: Instant
) {
    // Business logic methods
    fun confirm(): Booking = copy(status = BookingStatus.CONFIRMED)
}

// ✅ Use init blocks for validation
data class Seat(
    val row: String,
    val number: Int
) {
    init {
        require(row.matches(Regex("^[A-Z]$"))) { "Invalid row: $row" }
        require(number > 0) { "Seat number must be positive" }
    }
}

// ❌ Don't use data classes for classes with behavior-heavy logic
data class BookingService(...)  // Wrong! Not a data holder
```

### Use `copy()` for Immutable Updates

```kotlin
// ✅ Immutable updates with copy
fun confirmBooking(booking: Booking): Booking {
    return booking.copy(
        status = BookingStatus.CONFIRMED,
        confirmedAt = Instant.now()
    )
}

// ❌ Avoid mutation
fun confirmBooking(booking: Booking) {
    booking.status = BookingStatus.CONFIRMED  // Won't compile if val
}
```

---

## Functions

### Single Expression Functions

```kotlin
// ✅ Use = for single expressions
fun isAvailable(seat: Seat): Boolean =
    !repository.existsBooking(seat)

fun displayName(booking: Booking): String =
    "${booking.customer.name} - ${booking.seat.displayName()}"

// ✅ When for single expression returns
fun getPriceMultiplier(section: SeatSection) = when (section) {
    SeatSection.STANDARD -> 1.0
    SeatSection.VIP -> 1.5
    SeatSection.PREMIUM -> 2.0
}
```

### Extension Functions

```kotlin
// ✅ Use extensions to add domain-specific functionality
fun Booking.displayStatus(): String = when (status) {
    BookingStatus.PENDING -> "Awaiting confirmation"
    BookingStatus.CONFIRMED -> "Confirmed for ${showTime.movieTitle}"
    BookingStatus.CANCELLED -> "Cancelled"
}

// ✅ Extensions in domain logic
fun Instant.isInPast(): Boolean = this.isBefore(Instant.now())

// ❌ Don't use extensions to bypass encapsulation
fun Booking.forceConfirm() {
    this.status = BookingStatus.CONFIRMED  // Violates invariants!
}
```

### Named Arguments

```kotlin
// ✅ Use named arguments for clarity (especially with multiple params of same type)
val booking = Booking.create(
    id = BookingId("123"),
    customer = CustomerId("customer-1"),
    seat = Seat(row = "A", number = 12),
    showTime = showTimeRef
)

// ✅ Required for boolean parameters
repository.findBookings(
    includeExpired = false,
    onlyConfirmed = true
)

// ❌ Hard to read
repository.findBookings(false, true)
```

### Default Arguments

```kotlin
// ✅ Provide sensible defaults
fun createBooking(
    seat: Seat,
    createdAt: Instant = Instant.now(),
    status: BookingStatus = BookingStatus.PENDING
): Booking

// ✅ Use for optional behavior
fun findBookings(
    customerId: CustomerId,
    limit: Int = 50,
    offset: Int = 0
): List<Booking>
```

---

## Collections

### Prefer Immutable Collections

```kotlin
// ✅ Use immutable collections by default
val bookings: List<Booking> = repository.findAll()
val bookingMap: Map<BookingId, Booking> = bookings.associateBy { it.id }

// ✅ Use mutable only when needed
val mutableBookings = mutableListOf<Booking>()
mutableBookings.add(newBooking)

// ❌ Don't expose mutable collections
class BookingService {
    val bookings = mutableListOf<Booking>()  // Can be modified from outside!
}

// ✅ Expose immutable view
class BookingService {
    private val _bookings = mutableListOf<Booking>()
    val bookings: List<Booking> get() = _bookings.toList()
}
```

### Functional Collection Operations

```kotlin
// ✅ Use functional operators
val confirmedBookings = bookings.filter { it.status == BookingStatus.CONFIRMED }
val bookingIds = bookings.map { it.id }
val totalSeats = bookings.count()

// ✅ Use sequences for large collections
val expensiveBookings = bookings.asSequence()
    .filter { it.seat.section == SeatSection.VIP }
    .map { it.calculatePrice() }
    .toList()

// ✅ Use groupBy for categorization
val bookingsByStatus: Map<BookingStatus, List<Booking>> =
    bookings.groupBy { it.status }

// ❌ Avoid manual loops when functional operators work
val confirmed = mutableListOf<Booking>()
for (booking in bookings) {
    if (booking.status == BookingStatus.CONFIRMED) {
        confirmed.add(booking)
    }
}
```

---

## String Handling

### String Templates

```kotlin
// ✅ Use string templates
val message = "Booking ${booking.id} confirmed for ${customer.name}"

// ✅ Use ${} for expressions
val status = "Status: ${booking.status.name.lowercase()}"

// ❌ Avoid string concatenation
val message = "Booking " + booking.id + " confirmed"
```

### Multiline Strings

```kotlin
// ✅ Use raw strings for multiline
val email = """
    Hello ${customer.name},

    Your booking for ${showTime.movieTitle} is confirmed.
    Seat: ${seat.displayName()}
    Time: ${showTime.startTime}

    Enjoy the show!
""".trimIndent()
```

---

## Scope Functions

### When to Use Each

```kotlin
// ✅ let: Transform nullable to non-nullable
booking?.let { processBooking(it) }

// ✅ apply: Configure object
val booking = Booking(...).apply {
    println("Created booking: $id")
}

// ✅ also: Side effects in chain
val result = calculatePrice()
    .also { logger.info("Calculated price: $it") }
    .applyDiscount()

// ✅ run: Execute block with context
val isValid = booking.run {
    status == BookingStatus.PENDING && seat.isAvailable()
}

// ✅ with: Multiple calls on same object
val summary = with(booking) {
    "Booking $id for ${customer.name} at ${showTime.movieTitle}"
}

// ❌ Don't overuse - can hurt readability
val result = booking?.let { b ->
    b.seat.let { s ->
        s.section.let { sec ->
            sec.priceMultiplier()
        }
    }
}  // Too nested!

// ✅ Better
val result = booking?.seat?.section?.priceMultiplier()
```

---

## Error Handling

### Use Require/Check/Assert

```kotlin
// ✅ require: Validate arguments
fun bookSeat(seat: Seat, showTime: ShowTimeReference) {
    require(showTime.startTime.isAfter(Instant.now())) {
        "Cannot book seats for past showtimes"
    }
}

// ✅ check: Validate state
fun confirmBooking(booking: Booking) {
    check(booking.status == BookingStatus.PENDING) {
        "Only pending bookings can be confirmed"
    }
}

// ✅ requireNotNull: Handle nullability
fun processBooking(bookingId: BookingId) {
    val booking = requireNotNull(repository.findById(bookingId)) {
        "Booking $bookingId not found"
    }
}
```

### Domain Exceptions

```kotlin
// ✅ Custom exceptions for domain errors
sealed class BookingException(message: String) : RuntimeException(message)

class SeatNotAvailableException(seat: Seat) :
    BookingException("Seat ${seat.displayName()} is not available")

// ✅ Use in domain logic
fun bookSeat(seat: Seat): Booking {
    if (!isAvailable(seat)) {
        throw SeatNotAvailableException(seat)
    }
    // ...
}
```

---

## Testing Conventions

### Test Naming

```kotlin
// ✅ Use descriptive test names
@Test
fun `should confirm booking when status is pending`()

@Test
fun `should throw exception when confirming already confirmed booking`()

@Test
fun `should return success result when seat is available`()

// ❌ Unclear names
@Test
fun testConfirm()

@Test
fun test1()
```

### Test Structure

```kotlin
// ✅ Arrange-Act-Assert with clear sections
@Test
fun `should calculate VIP price correctly`() {
    // Arrange
    val seat = Seat(row = "A", number = 1, section = SeatSection.VIP)
    val basePrice = 10.0

    // Act
    val actualPrice = calculatePrice(seat, basePrice)

    // Assert
    assertEquals(15.0, actualPrice)
}
```

---

## Code Organization

### File Per Class

```kotlin
// ✅ One public class per file
// File: Booking.kt
data class Booking(...)

// ✅ Multiple related small classes OK
// File: BookingResult.kt
sealed interface BookingResult {
    data class Success(...) : BookingResult
    data class Failure(...) : BookingResult
}
```

### Top-Level Functions

```kotlin
// ✅ Use top-level for utility functions
// File: BookingExtensions.kt
fun Booking.isExpired(): Boolean =
    status == BookingStatus.PENDING &&
    createdAt.plus(15, ChronoUnit.MINUTES).isBefore(Instant.now())

// ❌ Avoid utility classes
object BookingUtils {
    fun isExpired(booking: Booking) = ...  // Use extension instead
}
```

---

## Anti-Patterns to Avoid

### ❌ Overusing `!!`
```kotlin
val booking = findBooking(id)!!  // Avoid!
```

### ❌ Mutable Properties in Data Classes
```kotlin
data class Booking(var status: BookingStatus)  // Breaks immutability!
```

### ❌ God Objects
```kotlin
class BookingManager {  // Does everything!
    fun create()
    fun confirm()
    fun cancel()
    fun findById()
    fun sendEmail()
    fun processPayment()
}
```

### ❌ Primitive Obsession
```kotlin
fun bookSeat(customerId: String, seatId: String)  // Use value classes!
```

---

## Code Formatting

Use the official Kotlin style guide (already configured in pom.xml):
```xml
<kotlin.code.style>official</kotlin.code.style>
```

Run formatting:
```bash
mvn kotlin:format
```

---

## Recommended IntelliJ Settings

- Enable: "Add unambiguous imports on the fly"
- Enable: "Optimize imports on the fly"
- Set: "Hard wrap at 120 columns"
- Enable: "Show parameter name hints"
