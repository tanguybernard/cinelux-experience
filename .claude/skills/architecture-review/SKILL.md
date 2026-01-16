---
name: architecture-review
description: Reviews code for Hexagonal Architecture and DDD compliance. Use when reviewing pull requests, implementing new features, or validating architecture follows project guidelines. Checks domain purity, dependency direction, port correctness, and ubiquitous language.
allowed-tools: Read, Grep, Glob, Bash(grep:*)
---

# Architecture Review Skill

Review code for **Hexagonal Architecture** and **Domain-Driven Design** compliance in the CineLux project.

## When to Use

- Reviewing new feature implementations
- Validating pull request changes
- Checking layer separation after refactoring
- Ensuring new bounded contexts follow patterns

## Review Checklist

### 1. Domain Layer Purity

The domain layer (`*/domain/`) must have **zero framework dependencies**.

**Check for forbidden imports:**
```bash
# Must return NOTHING
grep -r "import org.springframework" src/main/kotlin/com/cinelux/*/domain/
grep -r "import jakarta" src/main/kotlin/com/cinelux/*/domain/
grep -r "import javax" src/main/kotlin/com/cinelux/*/domain/
grep -r "import com.fasterxml.jackson" src/main/kotlin/com/cinelux/*/domain/
```

**Check for forbidden annotations:**
```bash
# Must return NOTHING
grep -rE "@(Component|Service|Repository|Entity|Table|Id|Column|RestController|Autowired|Bean|Configuration|Valid|NotNull|NotBlank)" src/main/kotlin/com/cinelux/*/domain/
```

**Allowed in domain:**
- Kotlin stdlib only
- `java.time.*` for temporal types
- `java.util.UUID` for ID generation
- Other domain classes within the same bounded context

### 2. Dependency Direction

Dependencies must flow **inward only**: `Infrastructure -> Ports -> Application -> Domain`

**Check domain doesn't import infrastructure:**
```bash
# Must return NOTHING
grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/domain/
grep -r "import.*application" src/main/kotlin/com/cinelux/*/domain/
```

**Check application doesn't import infrastructure:**
```bash
# Must return NOTHING
grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/application/
```

### 3. Port Correctness

**API Ports** (`application/port/api/`):
- Define what the application **can do** (driving side)
- Should be interfaces with command/query methods
- No framework annotations

**SPI Ports** (`application/port/spi/`):
- Define what the application **needs** (driven side)
- Repository interfaces, external service interfaces
- No framework annotations

**Check ports have no annotations:**
```bash
# Must return NOTHING
grep -rE "@(Component|Service|Repository)" src/main/kotlin/com/cinelux/*/application/port/
```

### 4. Infrastructure Responsibilities

**REST Controllers** (`infrastructure/api/rest/`):
- Spring annotations allowed
- Should only depend on API ports
- Use DTOs, never expose domain entities directly

**Persistence** (`infrastructure/spi/persistence/`):
- JPA annotations allowed
- Create separate JPA entities (never annotate domain entities)
- Implement SPI port interfaces
- Map between JPA entities and domain entities

**Check infrastructure implements ports:**
- Each `*Repository` in infrastructure should implement a port interface
- Each controller should inject API port interfaces, not use cases directly

### 5. Ubiquitous Language

**Booking Context terms:**
| Correct | Forbidden |
|---------|-----------|
| Booking | Reservation, Ticket, Order |
| Seat | Chair, Position |
| ShowTime | Screening, Schedule, Movie |
| Customer | User, Client |
| BookingStatus | State, Status (without Booking prefix) |

**Check for forbidden terms:**
```bash
# Review results - these terms should not appear in booking context code
grep -ri "class.*Reservation" src/main/kotlin/com/cinelux/booking/
grep -ri "class.*User[^N]" src/main/kotlin/com/cinelux/booking/  # UserN allows UserId
grep -ri "class.*Order" src/main/kotlin/com/cinelux/booking/
grep -ri "class.*Movie" src/main/kotlin/com/cinelux/booking/  # Use ShowTime, not Movie
```

### 6. Package Structure Validation

Expected structure for each bounded context:
```
context/
├── domain/
│   ├── model/          # Entities, Value Objects, Aggregates
│   ├── service/        # Domain services (optional)
│   ├── event/          # Domain events (optional)
│   └── exception/      # Domain exceptions
├── application/
│   ├── usecase/        # Use case implementations
│   └── port/
│       ├── api/        # Driving ports (interfaces)
│       └── spi/        # Driven ports (interfaces)
└── infrastructure/
    ├── api/
    │   └── rest/       # REST controllers
    ├── spi/
    │   └── persistence/ # JPA repositories
    └── config/         # Spring configuration
```

## Review Output Format

Provide a structured report:

```markdown
## Architecture Review Summary

### Domain Purity
- [ ] No framework imports
- [ ] No framework annotations
- [ ] Pure Kotlin logic only

### Dependency Direction
- [ ] Domain has no outward dependencies
- [ ] Application doesn't import infrastructure
- [ ] Ports are framework-agnostic

### Port Design
- [ ] API ports define use cases
- [ ] SPI ports define external dependencies
- [ ] No annotations on port interfaces

### Infrastructure
- [ ] Controllers use DTOs (not domain entities)
- [ ] JPA entities separate from domain entities
- [ ] Proper port implementations

### Ubiquitous Language
- [ ] Correct terminology used
- [ ] No forbidden terms

### Issues Found
1. [Issue description with file:line reference]
2. ...

### Recommendations
1. [Suggestion for improvement]
2. ...
```

## Common Violations

### Domain Entity with JPA Annotations
```kotlin
// VIOLATION in domain/model/
@Entity
@Table(name = "bookings")
data class Booking(...)
```
**Fix:** Move JPA entity to `infrastructure/spi/persistence/` and keep domain entity pure.

### Use Case with Spring Annotation
```kotlin
// VIOLATION in application/usecase/
@Service
class BookSeatUseCaseImpl(...)
```
**Fix:** Remove annotation. Wire use cases in `infrastructure/config/` using `@Bean`.

### Controller Exposing Domain Entity
```kotlin
// VIOLATION in infrastructure/api/rest/
@GetMapping("/{id}")
fun getBooking(): Booking  // Domain entity!
```
**Fix:** Create a DTO and map from domain entity.

### Port with Repository Annotation
```kotlin
// VIOLATION in application/port/spi/
@Repository
interface BookingRepository
```
**Fix:** Remove annotation. Add `@Repository` only to the implementation in infrastructure.

## References

- See `.claude/rules/hexagonal-architecture.md` for detailed layer rules
- See `.claude/rules/ddd-booking-context.md` for domain model guidelines