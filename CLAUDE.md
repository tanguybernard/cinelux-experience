# CineLux Experience - Cinema Booking System

## Project Vision

A long-term, production-grade cinema seat booking system built with **Domain-Driven Design** and **Hexagonal Architecture** principles using Kotlin and Spring Boot.

## Architecture Philosophy

### Hexagonal Architecture (Ports & Adapters)

We strictly enforce separation between:
- **Domain Layer**: Pure business logic, no framework dependencies
- **Application Layer**: Use cases orchestrating domain objects
- **Ports**: Interfaces defining contracts (input/output)
- **Adapters**: Framework implementations (REST, DB, messaging)

**Critical Rule**: Dependencies flow **inward only**
```
Adapters → Ports → Application → Domain
```

### Domain-Driven Design

Starting with **Booking Context** as our core bounded context.

**Ubiquitous Language** (Booking Context):
- `Booking`: Aggregate root representing a seat reservation
- `Seat`: Value object identifying a physical cinema seat
- `ShowTime`: When a movie is screened
- `BookingStatus`: States (Pending, Confirmed, Cancelled)
- `Customer`: Entity representing the person booking

**Future Contexts** (to be added as needed):
- Screening Context (movies, schedules, halls)
- Payment Context (transactions, pricing)
- Customer Context (accounts, loyalty)

## Package Structure

```
src/main/kotlin/com/cinelux/
├── booking/                    # Booking Bounded Context
│   ├── domain/                 # Pure business logic (no framework deps)
│   │   ├── model/              # Entities, Value Objects, Aggregates
│   │   ├── service/            # Domain services
│   │   └── exception/          # Domain exceptions
│   ├── application/            # Use cases
│   │   ├── usecase/            # Use case implementations
│   │   └── port/               # Port interfaces
│   │       ├── input/          # Driving ports (API contracts)
│   │       └── output/         # Driven ports (Repository, external service contracts)
│   └── adapter/                # Framework implementations
│       ├── input/
│       │   └── rest/           # REST controllers (Spring)
│       ├── output/
│       │   ├── persistence/    # JPA repositories, entities
│       │   └── messaging/      # Event publishers
│       └── config/             # Spring configuration

src/test/kotlin/com/cinelux/
├── booking/
│   ├── domain/                 # Unit tests (no Spring context)
│   ├── application/            # Use case tests (mocked ports)
│   └── adapter/                # Integration tests (Spring Boot Test)
```

## Technology Stack

- **Language**: Kotlin 2.1.20 (JVM 1.8+)
- **Build**: Maven
- **Framework**: Spring Boot 3.x (to be added)
- **Database**: TBD (PostgreSQL recommended)
- **Testing**: JUnit 5, MockK, Spring Boot Test

## Spring Boot Dependencies To Add

When adding Spring Boot, use:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.x</version>
</parent>

<!-- Add starters as needed -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>
```

## Coding Principles

1. **No framework in domain**: Domain layer NEVER imports Spring, JPA, Jackson, etc.
2. **Immutability by default**: Use `val`, data classes, immutable collections
3. **Explicit over implicit**: Prefer clear code over Kotlin magic
4. **Test-driven**: Write tests before implementation
5. **Ubiquitous language**: Code uses exact terms from domain experts

## Development Workflow

1. Start with domain model (entities, value objects)
2. Define ports (interfaces for use cases and repositories)
3. Implement use cases in application layer
4. Build adapters (REST, persistence) implementing ports
5. Write tests at each layer

## Architecture Validation

To check architecture compliance:
```bash
# Domain layer should have ZERO Spring/JPA imports
grep -r "@\(Component\|Service\|Repository\|Entity\|Table\|RestController\)" src/main/kotlin/com/cinelux/*/domain/
# Should return nothing

# Check dependency direction: domain shouldn't import from adapter
grep -r "import.*adapter" src/main/kotlin/com/cinelux/*/domain/
# Should return nothing
```

## Expanding to Multiple Contexts

When adding new bounded contexts:
1. Create new top-level package: `com.cinelux.screening/`
2. Duplicate the structure: domain/ → application/ → adapter/
3. Define context map relationships in `.claude/rules/context-map.md`
4. Use domain events for cross-context communication

## Getting Started

See `.claude/rules/` for detailed guidelines:
- `hexagonal-architecture.md` - Layer rules with Kotlin examples
- `ddd-booking-context.md` - Domain model design
- `kotlin-standards.md` - Language conventions
- `testing-strategy.md` - Test patterns per layer
