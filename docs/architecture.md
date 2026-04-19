# PropStack — Architecture

[English](architecture.md) | [Japanese (日本語)](architecture-ja.md)

---

## Overview

PropStack has two orthogonal concerns:

| Module | Responsibility |
|--------|---------------|
| `PropStack` | Cascading property resolution from a `List<PropertySource>` |
| `Registry` | Application-scoped component management via `ConcurrentHashMap` |

They are independent. `PropStack` reads strings. `Registry` manages objects. The application wires them together.

---

## PropStack — Stack Structure

### List\<PropertySource\>

`PropStack` holds an ordered `List<PropertySource>`. Resolution is first-match-wins: the list is iterated in order and the first non-empty value is returned.

```
sources[0]  in-memory overrides    (set() calls)          ← highest priority
sources[1]  SystemProperties       (-D flags)
sources[2]  EnvironmentVariables   (OS env)
sources[3]  ~/.appName/app.props   (user home, optional)
sources[n]  classpath app.props    (bundled defaults)     ← lowest priority
```

The list is a plain `java.util.ArrayList`. There is no insertion API — you manipulate it directly via `PropStack.defaultSources(appName)` and standard `List` operations (DD-006).

### PropertySource Interface

```java
public interface PropertySource {
    Optional<String> getRawValue(String key);
    PropertySource set(String key, String value);
    Set<String> keys();
    default Optional<String> get(String key) { ... }  // applies VariableExpander
}
```

All built-in sources are anonymous inner classes returned by static factory methods (`systemProperties()`, `environmentVariables()`, `fromPath()`, `fromClasspath()`, `of(Map)`, `fromArgs()`).

`fromPath()` silently ignores missing files — this is intentional for optional config locations, but means **typos in paths produce no error**. Use `trace()` to verify which sources contributed values.

### VariableExpander

`${VAR}` references in property values are expanded by `VariableExpander.INSTANCE`, applied as a `UnaryOperator<String>` in `PropertySource.get()`. Expansion sources are system properties and environment variables, in that order. Circular references are not detected.

---

## TypedKey — Record

`TypedKey<T>` is a Java `record`:

```java
public record TypedKey<T>(
    String key,
    Class<T> type,
    T defaultValue,
    String description,
    boolean sensitive
) { ... }
```

Being a `record` gives `TypedKey` structural equality, safe use as map keys, and immutable builder semantics (`.defaultsTo()` and `.describedAs()` return new instances).

### Type Conversion

`PropStack` converts raw `String` values to `T` internally via a private `convert()` method. Supported types:

| Factory | Java type |
|---------|-----------|
| `TypedKey.string()` | `String` |
| `TypedKey.integer()` | `Integer` / `int` |
| `TypedKey.bool()` | `Boolean` / `boolean` |
| `TypedKey.longKey()` | `Long` / `long` |
| `TypedKey.doubleKey()` | `Double` / `double` |
| `TypedKey.stringList()` | `List<String>` (comma-split) |
| `TypedKey.secret()` | `String` + `sensitive=true` |

No arbitrary object construction — `String → DataSource` is construction, not conversion (DD-004).

### defaultsTo() vs describedAs()

Two distinct concepts that look similar but behave differently (DD-008):

| Method | Effect on validate() | Effect on dump() | Semantics |
|--------|---------------------|------------------|-----------|
| `.defaultsTo(v)` | Skipped (key has value) | Shows `587 (default)` | Production-safe fallback |
| `.describedAs(text)` | Caught as missing | Shows `[MISSING] — text` | Documentation only |

A key with `.defaultsTo()` will never be reported missing. A key with only `.describedAs()` will always be caught if the value is absent.

### KeyHolder Pattern

Enums implement `KeyHolder` to group related keys and enable typed access:

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST").describedAs("SMTP server hostname")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
    PASSWORD(TypedKey.secret("SMTP_PASSWORD"));

    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

This pattern solves the Java enum generic limitation: a single enum cannot implement `TypedKey<T>` for mixed types (String, Integer, Boolean), but it can *hold* a `TypedKey<?>` field (DD-003).

---

## Registry — ConcurrentHashMap

```java
private static final Map<String, Object> instances = new ConcurrentHashMap<>();
```

`Registry` is a static class backed by a single `ConcurrentHashMap<String, Object>`. The map key is a string computed from the lookup identity:

| Lookup | Map key format |
|--------|---------------|
| `Registry.get(DataSource.class)` | `"com.example.DataSource"` |
| `Registry.get(DB.PROD)` | `"com.example.DataSource#PROD"` |
| `Registry.get("myName")` | `"myName"` |

### Thread Safety

All operations on `ConcurrentHashMap` are atomic at the single-entry level. `computeIfAbsent` (used for lazy initialization) guarantees the supplier runs at most once per key.

### Test Isolation

`Registry.clear()` resets all state. Call in `@AfterEach` to prevent cross-test contamination.

---

## trace() Behavior

`trace()` iterates sources and **stops at the first MATCH**. Sources after the match are not shown:

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db  ← MATCH
```

Sources [3] and [4] are not printed, even if they also contain the key. This reflects the resolution behavior exactly — but it means you cannot use `trace()` to see all layers simultaneously. If you need that, iterate `PropStack.defaultSources()` manually.

---

## Data Flow

```
application.properties  ─┐
user overrides           ─┤  List<PropertySource>  →  PropStack.get(key)
env vars / -D flags      ─┤                             │
set() overrides          ─┘                             │
                                                        ▼
                                              TypedKey.convert(String → T)
                                                        │
                                                        ▼
                                              T (String / int / boolean / List / ...)

PropStack.validate()  → collect all missing TypedKey (no default, no value) → throw once
PropStack.dump()      → format all KeyHolder entries with value/default/secret/missing
PropStack.trace()     → walk sources for one key, stop at first match
```

---

## 1.0 Remaining Tasks

> CI badge is not yet configured. The following are tracked for 1.0.

- [ ] GitHub Actions workflow (`mvn verify` on push/PR)
- [ ] CI badge in README
- [ ] Javadoc site (GitHub Pages)
- [ ] `PropStack.defaultSources()` promoted to stable API documentation
- [ ] Evaluate: `trace()` option to show all sources (not stop at first match)
- [ ] Evaluate: `TypedKey` registration for `validate()` without enum (anonymous key catalog)
