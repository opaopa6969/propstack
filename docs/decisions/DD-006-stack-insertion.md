# DD-006: Stack Insertion — defaultSources() over Inserter

**Status:** Accepted  
**Date:** 2026-04-05  
**DGE Session:** [005-fraud-alert-features](../../dge/sessions/2026-04-05-005-fraud-alert-features.md)

---

## Decision

Expose the default source list as a mutable `ArrayList` via `PropStack.defaultSources(appName)`. Users manipulate it with standard `List` operations.

## Context

Advanced users sometimes need to insert a custom `PropertySource` at a specific position in the resolution stack — for example, a Vault source after environment variables but before the classpath file.

```
set() → -D → env → [VAULT HERE] → home file → classpath
```

## Candidates Evaluated

| Approach | Verdict | Reason |
|----------|---------|--------|
| A: Inserter pattern (predicate-based: "insert after env") | Rejected | Depends on internal implementation names |
| B: Full manual construction | Already possible | Tedious to replicate default config |
| C: Index-based `insertAt(2, source)` | Rejected | Fragile — breaks if internal order changes |
| D: Named sources + `insertAfter("env")` | Rejected | Over-engineering; string names are weak |
| **E: `defaultSources()` static helper** | **Accepted** | Zero new concepts; standard Java List |

## Decision: Option E

```java
public static ArrayList<PropertySource> defaultSources(String appName) {
    var list = new ArrayList<>(List.of(
        PropertySource.systemProperties(),
        PropertySource.environmentVariables()));
    if (appName != null && !appName.isEmpty()) {
        list.add(PropertySource.fromPath(...));
    }
    list.add(PropertySource.fromClasspath("application.properties"));
    return list;
}
```

Usage:

```java
var sources = PropStack.defaultSources("myapp");
sources.add(2, new VaultPropertySource(vaultClient));  // standard List.add
PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

## Rationale

- **Zero new concepts:** `List.add(index, element)` is known by every Java developer
- **Mutable ArrayList:** the caller owns the list; no special insert API needed
- **Order visible:** the caller can `System.out.println(sources)` to see the stack
- **No naming dependency:** index 2 is relative to the list the caller received

## Consequences

- If the default order changes in a future release, callers using index-based insertion may need to update their code. This is acceptable — the method returns the *current* default list for inspection.
- The `PropStack(boolean, PropertySource...)` constructor accepts the final array. This is stable API.
- 99% of users never need this. The method is there for the 1% who do.
