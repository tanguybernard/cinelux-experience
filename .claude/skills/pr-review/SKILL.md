---
name: pr-review
description: Comprehensive PR review for Spring Kotlin projects. Spawns security and performance agents in parallel for deep analysis. Use before merging pull requests or after significant changes.
allowed-tools: Read, Grep, Glob, Bash(git:*), Task
---

# PR Review Skill

Comprehensive code review for **Spring Kotlin** projects with **DDD** and **Hexagonal Architecture**. This skill spawns specialized agents in parallel for deep security and performance analysis.

## When to Use

- Before merging a pull request
- After significant feature implementation
- When refactoring critical paths
- Before releases

## Review Strategy

This skill performs a **two-phase review**:

### Phase 1: Quick Context Gathering

First, gather context about what changed:

```bash
# Get list of changed files (if on a branch)
git diff --name-only main...HEAD 2>/dev/null || git diff --name-only HEAD~5
```

### Phase 2: Parallel Agent Analysis

Spawn **two specialized agents in parallel** using the Task tool:

1. **Security Agent** (`security-reviewer`) - OWASP checks, input validation, auth issues
2. **Performance Agent** (`performance-reviewer`) - JPA issues, transaction scope, memory patterns

**IMPORTANT**: You MUST spawn both agents in a SINGLE message to run them in parallel.

---

## Execution Instructions

When this skill is invoked, you MUST:

1. **Gather context** - Run git diff to see what files changed

2. **Spawn both agents in parallel** - Use Task tool with BOTH agents in a SINGLE message:

```
Task 1 (Security):
- subagent_type: "Explore"
- prompt: |
    You are the security-reviewer agent. See .claude/agents/security-reviewer.md for full instructions.

    Focus on these recently changed files:
    {list of changed files from git diff}

    Analyze the codebase following the security review checklist.
    Return findings in the specified output format.

Task 2 (Performance):
- subagent_type: "Explore"
- prompt: |
    You are the performance-reviewer agent. See .claude/agents/performance-reviewer.md for full instructions.

    Focus on these recently changed files:
    {list of changed files from git diff}

    Analyze the codebase following the performance review checklist.
    Return findings in the specified output format.
```

3. **Aggregate results** - Combine findings from both agents into a unified report

4. **Present summary** - Show the user a consolidated PR review

---

## Agents Used

This skill uses agents defined in `.claude/agents/`:

| Agent | File | Purpose |
|-------|------|---------|
| security-reviewer | `.claude/agents/security-reviewer.md` | OWASP, input validation, auth, secrets |
| performance-reviewer | `.claude/agents/performance-reviewer.md` | N+1 queries, transactions, caching |

---

## Output Format

Present the final review as:

```markdown
## PR Review Summary

### Security Findings
[Results from Security Agent]

### Performance Findings
[Results from Performance Agent]

### Overall Assessment

**Ready to merge?** Yes/No

**Blocking Issues**: X
**Warnings**: Y
**Recommendations**: Z

### Action Items
1. [ ] [Most critical item]
2. [ ] [Next item]
...
```

---

## Complementary Skills

For complete PR review coverage, consider also running:
- `/architecture-review` - Hexagonal Architecture and DDD compliance
