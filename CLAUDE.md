# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CineLux Experience is a cinema seat booking system built with **Domain-Driven Design** and **Hexagonal Architecture** in Kotlin. It is in early stages: domain models and use cases are implemented, but the infrastructure layer (REST, JPA, Spring Boot) is not yet wired in.

## Build & Test Commands

```bash
# Build
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=com.cinelux.booking.application.usecase.FindAvailableSeatsUseCaseImplTest

# Run a single test method
mvn test -Dtest="com.cinelux.architecture.HexagonalArchitectureTest#domain should not depend on application"

# Run only architecture tests
mvn test -Dtest=com.cinelux.architecture.HexagonalArchitectureTest

# Run only Cucumber acceptance tests (booking)
mvn test -Dtest=com.cinelux.booking.acceptance.CucumberAcceptanceTest

# Run only Cucumber acceptance tests (screening)
mvn test -Dtest=com.cinelux.screening.acceptance.CucumberScreeningAcceptanceTest

# Cucumber reports output to target/cucumber-reports/
```

## Architecture

### Hexagonal Architecture (strictly enforced by ArchUnit tests)

Dependencies flow **inward only**: `Infrastructure → Ports → Application → Domain`

Each bounded context follows this package structure:
```
com.cinelux.<context>/
├── domain/model/          # Entities, Value Objects, Aggregates (pure Kotlin, NO framework deps)
├── application/
│   ├── usecase/           # Use case implementations (*UseCaseImpl)
│   └── port/
│       ├── api/           # Driving ports: *UseCase, *Command, *Query, *Result
│       └── spi/           # Driven ports: *Repository, *Gateway, *Client, *Publisher
└── infrastructure/        # REST controllers, JPA, messaging, Spring config (not yet implemented)
```

**Critical constraints** (validated in `HexagonalArchitectureTest`):
- Domain layer MUST NOT import Spring, JPA, Jackson, or validation annotations
- Application/port layers MUST NOT import Spring or infrastructure
- Use `require`/`check` for domain validation, never annotation-based validation
- Use cases must be framework-agnostic (wired via infrastructure config, not `@Service`)

### Bounded Contexts

- **Booking** (`com.cinelux.booking`) — seat reservations. Aggregate root: `Booking`.
- **Screening** (`com.cinelux.screening`) — movie showtimes and halls. Aggregate root: `ShowTime`.

Cross-context references use Anti-Corruption Layer pattern: Booking references Screening data via `ShowTimeReference`, not the Screening domain's `ShowTime` entity.

### Naming Conventions

| Location | Suffix pattern |
|---|---|
| `port/api/` | `*UseCase`, `*Command`, `*Query`, `*Result` |
| `port/spi/` | `*Repository`, `*Gateway`, `*Client`, `*Publisher` |
| `application/usecase/` | `*UseCaseImpl` |

### Key Domain Patterns

- **Value Objects**: `@JvmInline value class` for IDs (`BookingId`, `CustomerId`, `ShowTimeId`)
- **Validation**: `init { require(...) }` blocks in domain objects
- **Result types**: Sealed interfaces for use case results (e.g., `BookSeatResult` with `Success`, `SeatAlreadyBooked`, etc.)
- **Immutability**: `val`, `data class`, immutable collections throughout

## Testing

Three test layers:
1. **Domain unit tests** (`<context>/domain/`) — pure domain logic, no mocks needed
2. **Use case tests** (`<context>/application/usecase/`) — use fake/in-memory repository implementations
3. **BDD acceptance tests** (`<context>/acceptance/`) — Cucumber `.feature` files in `src/test/resources/features/<context>/`

Acceptance tests use:
- Cucumber 7.15 with JUnit Platform Suite
- Shared `TestContext` singletons holding fake repositories
- `@wip` tag to exclude work-in-progress scenarios
- AssertJ for assertions

ArchUnit tests in `com.cinelux.architecture` enforce all architecture rules automatically.
