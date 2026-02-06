# CineLux Experience - Cinema Booking System

## Project Vision

A long-term, production-grade cinema seat booking system built with **Domain-Driven Design** and **Hexagonal Architecture** principles using Kotlin and Spring Boot.

## Architecture Philosophy

### Hexagonal Architecture (Ports & Adapters)

We strictly enforce separation between:
- **Domain Layer**: Pure business logic, no framework dependencies
- **Application Layer**: Use cases orchestrating domain objects
- **Ports**: Interfaces defining contracts (API = driving / SPI = driven)
- **Infrastructure**: Framework implementations (REST, DB, messaging)

**Terminology**:
- **API ports** (`port/api/`) = Driving ports (what application exposes) - Application Programming Interface
- **SPI ports** (`port/spi/`) = Driven ports (what application needs) - Service Provider Interface

**Critical Rule**: Dependencies flow **inward only**
```
Infrastructure → Ports → Application → Domain
```

### Domain-Driven Design

The system is organized into **bounded contexts**, each owning its domain model.

**Bounded Contexts**: Booking, Screening
**Future Contexts**: Payment, Customer

See `.claude/rules/ddd-strategic-design.md` for cross-context rules and context map.
See `.claude/rules/ddd-booking-context.md` and `.claude/rules/ddd-screening-context.md` for context-specific domain models and invariants.

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
│   │       ├── api/            # API ports - Driving ports (what application exposes)
│   │       └── spi/            # SPI ports - Driven ports (what application needs)
│   └── infrastructure/         # Framework implementations
│       ├── api/
│       │   └── rest/           # REST controllers (Spring)
│       ├── spi/
│       │   ├── persistence/    # JPA repositories, entities
│       │   └── messaging/      # Event publishers
│       └── config/             # Spring configuration
│
├── screening/                  # Screening Bounded Context
│   ├── domain/
│   │   └── model/              # ShowTime, Hall, ShowTimeId
│   ├── application/
│   │   ├── usecase/            # ListShowTimesUseCaseImpl
│   │   └── port/
│   │       ├── api/            # ListShowTimesUseCase
│   │       └── spi/            # ShowTimeRepository
│   └── infrastructure/         # (future: REST, persistence)

src/test/kotlin/com/cinelux/
├── booking/
│   ├── domain/                 # Unit tests (no Spring context)
│   ├── application/            # Use case tests (mocked ports)
│   └── acceptance/             # BDD acceptance tests (Cucumber)
│
├── screening/
│   ├── domain/                 # Unit tests for ShowTime, Hall
│   └── acceptance/             # BDD acceptance tests (Cucumber)
│
└── architecture/               # ArchUnit architecture tests
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
4. Build infrastructure (REST, persistence) implementing ports
5. Write tests at each layer

## Claude Workflow

When implementing new features, Claude must:

1. **Create a plan file** (e.g., `PLAN.md`) with:
   - Implementation steps
   - Architectural decisions
   - Files to create/modify
   - Any open questions

2. **Wait for approval** before writing any code

3. **Delete the plan file** after implementation is complete (optional)

## Architecture Validation

See `.claude/rules/hexagonal-architecture.md` for detailed layer rules and validation commands.
Use `/architecture-review` skill for comprehensive compliance checks.

## Expanding to Multiple Contexts

When adding new bounded contexts (see `screening/` as example):
1. Create new top-level package: `com.cinelux.<context>/`
2. Duplicate the structure: domain/ → application/ → infrastructure/
3. Update context map in `.claude/rules/ddd-strategic-design.md`
4. Create a DDD rules file: `.claude/rules/ddd-<context>-context.md`
5. Use domain events for cross-context communication
6. Add separate Cucumber test runner for the new context

## Getting Started

See `.claude/rules/` for detailed guidelines:
- `hexagonal-architecture.md` - Layer rules with Kotlin examples
- `ddd-strategic-design.md` - Context map, cross-context rules, general DDD patterns
- `ddd-booking-context.md` - Booking domain model and invariants
- `ddd-screening-context.md` - Screening domain model and invariants
- `kotlin-standards.md` - Language conventions
- `testing-strategy.md` - Test patterns per layer
