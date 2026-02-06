---
name: feature
description: Start a new feature with proper planning workflow. Use when implementing new functionality, adding use cases, or making significant changes. Enforces plan-first approach with architecture compliance.
allowed-tools: Read, Grep, Glob, Bash(git:*), Bash(mvn:*), Task, EnterPlanMode
---

# Feature Development Workflow

This skill enforces a structured approach to feature development following **Hexagonal Architecture** and **DDD** principles.

## When to Use

- Implementing a new use case
- Adding domain entities or value objects
- Creating new API endpoints
- Adding infrastructure adapters
- Any change touching multiple layers

## Workflow Phases

### Phase 1: Understand the Request

First, clarify the feature requirements:

1. **Parse the feature request** - What is being asked?
2. **Identify the bounded context** - Which context does this belong to?
3. **Check ubiquitous language** - Are we using correct domain terms?

If requirements are unclear, ask clarifying questions before proceeding.

---

### Phase 2: Explore the Codebase

Before planning, understand the current state:

```bash
# Check existing structure
tree src/main/kotlin/com/cinelux/ -d -L 3

# Find related domain entities
grep -r "data class" src/main/kotlin/com/cinelux/*/domain/model/

# Find existing use cases
grep -r "interface.*UseCase" src/main/kotlin/com/cinelux/*/application/port/api/

# Find existing ports
ls -la src/main/kotlin/com/cinelux/*/application/port/spi/
```

Identify:
- Related existing entities
- Ports that might need extension
- Patterns already in use

---

### Phase 3: Enter Plan Mode

**IMPORTANT**: You MUST enter plan mode before writing any code.

Use the `EnterPlanMode` tool to:
1. Design the implementation approach
2. Create `PLAN.md` in the project root
3. Wait for user approval

---

### Phase 4: Create the Plan

Write `PLAN.md` with the following structure:

```markdown
# Feature: [Feature Name]

## Summary
[1-2 sentence description]

## Affected Layers

### Domain Layer (`domain/`)
- [ ] New entities/value objects
- [ ] Domain services
- [ ] Domain events
- [ ] Domain exceptions

### Application Layer (`application/`)
- [ ] API ports (use cases)
- [ ] SPI ports (repositories, external services)
- [ ] Use case implementations

### Infrastructure Layer (`infrastructure/`)
- [ ] REST controllers + DTOs
- [ ] JPA entities + repositories
- [ ] Configuration

## Implementation Steps

1. **Domain Model**
   - Create/modify: `domain/model/...`
   - Validation rules: ...

2. **Ports**
   - API port: `application/port/api/...`
   - SPI port: `application/port/spi/...`

3. **Use Case**
   - Implement: `application/usecase/...`

4. **Infrastructure**
   - Controller: `infrastructure/api/rest/...`
   - Repository: `infrastructure/spi/persistence/...`
   - Config: `infrastructure/config/...`

5. **Tests**
   - Domain tests: `test/.../domain/...`
   - Use case tests: `test/.../application/...`
   - Integration tests: `test/.../infrastructure/...`

## Architecture Checklist

- [ ] Domain layer has no framework imports
- [ ] Ports are plain interfaces (no annotations)
- [ ] Use cases don't import infrastructure
- [ ] DTOs used at API boundaries (not domain entities)
- [ ] JPA entities separate from domain entities

## Open Questions
- [Any decisions needed from the user]
```

---

### Phase 5: Wait for Approval

After creating the plan:

1. Use `ExitPlanMode` to present the plan
2. **STOP and wait** for user approval
3. Do NOT write any code until approved

User can:
- Approve: "looks good" / "proceed"
- Request changes: "change X to Y"
- Ask questions: "why did you choose X?"

---

### Phase 6: Implement (After Approval)

Once approved, implement in this order:

#### 6.1 Domain Layer First
```kotlin
// Pure Kotlin, no framework dependencies
// Entities, Value Objects, Domain Services
```

#### 6.2 Ports Second
```kotlin
// Interfaces only, no implementations
// API ports (use cases) + SPI ports (repositories)
```

#### 6.3 Application Layer
```kotlin
// Use case implementations
// Depend only on domain and ports
```

#### 6.4 Infrastructure Last
```kotlin
// Spring annotations allowed here
// Controllers, JPA entities, repositories
```

#### 6.5 Tests at Each Layer
```bash
# Run tests after each layer
./mvnw test -Dtest="**/domain/**"
./mvnw test -Dtest="**/application/**"
./mvnw test
```

---

### Phase 7: Validate Architecture

After implementation, run architecture validation:

```bash
# Domain purity check
grep -r "import org.springframework" src/main/kotlin/com/cinelux/*/domain/
grep -r "import jakarta" src/main/kotlin/com/cinelux/*/domain/
# Must return NOTHING

# Dependency direction check
grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/domain/
grep -r "import.*infrastructure" src/main/kotlin/com/cinelux/*/application/
# Must return NOTHING

# Run all tests
./mvnw test
```

Consider running `/architecture-review` for comprehensive validation.

---

### Phase 8: Cleanup

After successful implementation:

1. Delete `PLAN.md`
2. Summarize what was implemented
3. Suggest next steps if any

---

## Layer Rules Reference

| Layer | Can Import | Cannot Import |
|-------|------------|---------------|
| Domain | Kotlin stdlib, java.time, java.util.UUID | Spring, JPA, Jackson, Application, Infrastructure |
| Application | Domain, Ports | Infrastructure, Spring |
| Ports | Domain | Infrastructure, Spring annotations |
| Infrastructure | Everything | - |

## Ubiquitous Language

See `.claude/rules/ddd-booking-context.md` for correct/forbidden terms per bounded context.
Always verify terminology compliance before planning and implementing.

---

## Example Usage

```
User: /feature add ability to cancel a booking

Claude:
1. Explores existing Booking entity and use cases
2. Enters plan mode
3. Creates PLAN.md with:
   - Domain: Add cancel() method to Booking, CancelledAt field
   - Port: CancelBookingUseCase interface
   - Use case: CancelBookingUseCaseImpl
   - Controller: DELETE /api/bookings/{id}
   - Tests for each layer
4. Waits for approval
5. Implements after approval
6. Validates architecture
7. Cleans up
```

---

## Related Skills

- `/architecture-review` - Validate hexagonal architecture compliance
- `/pr-review` - Full PR review with security and performance checks