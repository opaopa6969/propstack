# DD-003: TypedKey — Enum with TypedKey Field (Option D)

**Status:** Accepted  
**Date:** 2026-04-05  
**DGE Session:** [003-typedkey-enum](../../dge/sessions/2026-04-05-003-typedkey-enum.md)

---

## Decision

Enums implement `KeyHolder` and hold a `TypedKey<?>` as a field. PropStack performs type conversion internally.

## Context

Type-safe property access requires associating a Java type with each property key. The naive approach — having enum constants directly carry type information — fails because Java enums cannot have different generic type parameters per constant.

```java
// This doesn't work — enum can't mix TypedKey<String> and TypedKey<Integer>
enum Smtp {
    HOST(TypedKey.string("SMTP_HOST")),   // TypedKey<String>
    PORT(TypedKey.integer("SMTP_PORT")),  // TypedKey<Integer>   ← compile error
}
```

## Candidates Evaluated

| Approach | Type-safe | Enum | Feature grouping | Default values | Enumerable |
|----------|-----------|------|------------------|----------------|------------|
| A: TypedKey\<T\> interface on enum | Yes | No (1 type limit) | No | Yes | No |
| B: Static constants | Yes | No | No | Yes | No |
| C: Enum + converter | No (Object) | Yes | Yes | Yes | Yes |
| **D: Enum + TypedKey field** | **Yes** | **Yes** | **Yes** | **Yes** | **Yes** |

## Decision: Option D

Each enum constant holds a `TypedKey<?>` field. The type is captured at definition time via factory methods:

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587));

    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

PropStack casts internally:

```java
@SuppressWarnings("unchecked")
public <T> T get(KeyHolder holder) {
    return (T) get(holder.typedKey());
}
```

The `@SuppressWarnings("unchecked")` is safe because the type is captured at definition time and the caller's generic context matches.

## Consequences

- Boilerplate: every enum needs a 3-line constructor and `typedKey()` implementation
- Grouping: all SMTP keys are in one enum — easy to `validate(Smtp.class)` together
- `validate()` and `dump()` work by iterating enum constants via reflection
- The `KeyHolder` interface enables compile-time safety without runtime magic

## Boilerplate Trade-off

The 3-line constructor is intentional. It makes the pattern explicit and copyable. There is no code generation required. The IDE can generate it.

A future version could explore `@TypedKeyEnum` annotation processing, but the current boilerplate is acceptable for a zero-dependency library.
