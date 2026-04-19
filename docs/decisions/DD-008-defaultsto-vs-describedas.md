# DD-008: defaultsTo() vs describedAs() — Doc as Code

**Status:** Accepted  
**Date:** 2026-04-05  
**DGE Session:** [006-competitive-analysis](../../dge/sessions/2026-04-05-006-competitive-analysis.md)

---

## Decision

Separate safe production defaults (`.defaultsTo()`) from documentation hints (`.describedAs()`). The two-arg factory `TypedKey.string(key, default)` is deprecated.

## Context

The original `TypedKey.string("DB_HOST", "localhost")` two-arg form was ambiguous:

- Is `"localhost"` a safe production default? If so, `validate()` should skip it.
- Is `"localhost"` just a hint for documentation? If so, `validate()` should catch it and return `null` at runtime.

This ambiguity caused real bugs: a key defined with a doc hint would silently connect to `localhost` in production if the environment variable was forgotten.

## The Problem

```java
// Old — ambiguous. Is "localhost" safe for production? Nobody knows.
HOST(TypedKey.string("DB_HOST", "localhost"))
```

Two developers read this differently:
- Developer A: "localhost is the default — it's fine if DB_HOST isn't set"
- Developer B: "localhost is just a hint for newcomers — DB_HOST must be set in prod"

Both are wrong. The behavior is undefined by the API.

## Solution

Two explicit methods with unambiguous semantics:

```java
// Safe default — production-safe value, validate() skips this key
PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587))

// Documentation only — validate() catches missing, dump() shows description
HOST(TypedKey.string("DB_HOST").describedAs("database hostname, e.g. prod-db.internal"))

// Both — safe default + description for dump() output
PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587).describedAs("SMTP server port"))
```

## dump() Output

```
SMTP_PORT     = 587 (default)
DB_HOST       = [MISSING] — database hostname, e.g. prod-db.internal
DB_PASSWORD   = [MISSING] — app password (secret)
```

## validate() Behavior

| Key definition | validate() behavior |
|----------------|---------------------|
| `.defaultsTo(587)` | Skipped — has a value |
| `.describedAs(text)` | Caught if no env/file/set() value |
| No default, no description | Caught if no value |
| `.defaultsTo(587).describedAs(text)` | Skipped — has a value |

## Deprecation

`TypedKey.string(key, default)` and `TypedKey.integer(key, default)` two-arg factories are `@Deprecated`. They still work but emit IDE warnings. Migrate to:

```java
// Before
HOST(TypedKey.string("DB_HOST", "localhost"))

// After (explicit intent)
HOST(TypedKey.string("DB_HOST").describedAs("database hostname"))  // if it must be set
// or
HOST(TypedKey.string("DB_HOST").defaultsTo("localhost"))          // if localhost is safe in prod
```

## Consequences

- Clearer intent at definition time — the key author states their intent explicitly
- `validate()` is predictable — no surprises about what gets caught
- `dump()` output is informative — missing keys show why they're needed
- Minor migration cost for existing code using two-arg factories
