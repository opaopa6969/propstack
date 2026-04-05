# Design Decisions

[English](design-decisions.md) | [Japanese (日本語)](design-decisions.ja.md)

Design decisions made through [DGE (Design-Gap Exploration)](https://github.com/opaopa6969/DGE-toolkit) — character-driven dialogue for finding design gaps.

## DD-001: Why Not DI?

See [README — Why Not DI?](../README.md#why-not-di--a-dialogue)

**Decision:** PropStack uses a Registry pattern, not a DI framework.
**Rationale:** DI the principle is correct; DI frameworks are overkill. Spring's `@Autowired` has the same problems as Service Locator, plus proxy black magic.

## DD-002: Naming — PropStack

**Decision:** Named "PropStack" instead of alternatives.
**Candidates:** stackable-properties, unlaxer-config, propstack, cascading-config, konfig, simplestack
**Rationale:** Short, no naming conflicts, `new PropStack()` reads well. The name doesn't need to describe the full feature set — it's a small library by someone who hates DI.

## DD-003: TypedKey — Enum with TypedKey Field (案 D)

**Decision:** Enums implement `KeyHolder` and hold `TypedKey<?>` as a field.

**Problem:** Java enums can only implement one generic type parameter. A config group (e.g. SMTP) has String, Integer, and Boolean keys in the same enum.

**Candidates:**

| | Type-safe | Enum | Feature grouping | Default values | Enumeration |
|---|---|---|---|---|---|
| A: `TypedKey<T>` interface on enum | Yes | No (1 type limit) | No | Yes | No |
| B: Static constants | Yes | No | No | Yes | No |
| C: Enum + converter | No (Object) | Yes | Yes | Yes | Yes |
| **D: Enum + TypedKey field** | **Yes** | **Yes** | **Yes** | **Yes** | **Yes** |

**Solution:** Each enum constant holds a `TypedKey<?>` field. The type information is captured at definition time via factory methods (`TypedKey.string()`, `TypedKey.integer()`, etc.). PropStack performs the conversion internally.

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST")),
    PORT(TypedKey.integer("SMTP_PORT", 587));
    
    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}

String host = props.get(Smtp.HOST);  // compile-time String
int port = props.get(Smtp.PORT);     // compile-time int
```

## DD-004: No Object Construction in PropStack

**Decision:** PropStack does NOT support converting strings to arbitrary objects (e.g. DataSource, Cache implementations).

**Rejected approaches:**

| Approach | Why rejected |
|----------|-------------|
| Converter-attached TypedKey (`TypedKey.of("DB_URL", url -> new HikariDataSource(url))`) | Construction logic leaks into key definitions. Responsibility violation. |
| FQDN reflection (`com.example.RedisCacheImpl` → `Class.forName().newInstance()`) | Security risk. Black magic. |

**Rationale (DGE dialogue summary):**

> ⚔ **Rivai**: `String → int` is conversion. `String → DataSource` is **construction**. Different things. Don't mix them.
>
> 🏥 **Dr. House**: What you really want is a bridge between PropStack and Registry. That bridge is your `main()` method. 10 lines. Explicit. Debuggable.
>
> ☕ **Yana**: The best implementation is zero lines of implementation. PropStack reads strings. Registry manages objects. The app wires them together.

**The right way:**

```java
// main.java — you are the DI container
PropStack props = new PropStack();

Registry.put(DataSource.class, () -> createDataSource(props));
Registry.put(Cache.class, () -> {
    String impl = props.require(Config.CACHE_IMPL);  // read FQDN as string
    return (Cache) Class.forName(impl).getDeclaredConstructor().newInstance();  // app's choice
});

app.start();
```

**Boundary:** PropStack reads strings. Registry manages objects. Construction is the app's responsibility.
