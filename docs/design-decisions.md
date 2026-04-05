# Design Decisions

[English](design-decisions.md) | [Japanese (日本語)](design-decisions.ja.md)

Design decisions made through [DGE (Design-Gap Exploration)](https://github.com/opaopa6969/DGE-toolkit) — character-driven dialogue for finding design gaps.

Each DD links to the DGE session that produced it in [dge/sessions/](../dge/sessions/).

## DD-001: Why Not DI?

**DGE Session:** [002-why-not-di](../dge/sessions/2026-04-05-002-why-not-di.md)
See also [README — Why Not DI?](../README.md#why-not-di--a-dialogue)

**Decision:** PropStack uses a Registry pattern, not a DI framework.
**Rationale:** DI the principle is correct; DI frameworks are overkill. Spring's `@Autowired` has the same problems as Service Locator, plus proxy black magic.

## DD-002: Naming — PropStack

**DGE Session:** [001-naming](../dge/sessions/2026-04-05-001-naming.md)
**Decision:** Named "PropStack" instead of alternatives.
**Candidates:** stackable-properties, unlaxer-config, propstack, cascading-config, konfig, simplestack
**Rationale:** Short, no naming conflicts, `new PropStack()` reads well. The name doesn't need to describe the full feature set — it's a small library by someone who hates DI.

## DD-003: TypedKey — Enum with TypedKey Field (案 D)

**DGE Session:** [003-typedkey-enum](../dge/sessions/2026-04-05-003-typedkey-enum.md)
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

**DGE Session:** [004-no-object-construction](../dge/sessions/2026-04-05-004-no-object-construction.md)
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

## DD-005: Features from fraud-alert ApplicationProperties

**DGE Session:** [005-fraud-alert-features](../dge/sessions/2026-04-05-005-fraud-alert-features.md)
**Context:** The original ApplicationProperties (fraud-alert) has additional features not yet in PropStack. DGE session evaluated each.

**Adopted:**

| Feature | Design |
|---------|--------|
| Command-line args `--KEY=value` | `new PropStack(args)` — new constructor that adds args as highest-priority source |
| Explicit profile | `new PropStack("myapp", "prod")` — loads `application.prod.properties` in addition to base |
| `validate()` bulk check | `props.validate(Smtp.class, Db.class)` — reports ALL missing keys at once, not just the first |

**Adopted with opt-in:**

| Feature | Design |
|---------|--------|
| Auto-detect profile (user/host/os) | `new PropStack("myapp", PropStack.autoProfile())` — opt-in only, not default. Loads `application.user_opa.properties` etc. |

**Rejected:**

| Feature | Why |
|---------|-----|
| `getInstance()` FQDN reflection | DD-004: responsibility violation |
| `Populator` auto-bind | Becomes a DI framework |
| `getEnum()` type-based lookup | Niche. Add when needed |
| Auto-detect as default | Implicit behavior, hard to debug for newcomers. Opt-in is fine. |

## DD-006: Stack Insertion — defaultSources() over Inserter

**DGE Session:** [005-fraud-alert-features](../dge/sessions/2026-04-05-005-fraud-alert-features.md) (same session as DD-005)
**Problem:** Users sometimes need to insert a custom PropertySource (e.g. Vault, Consul) at a specific position in the resolution stack.

**Candidates:**

| Approach | Verdict | Reason |
|----------|---------|--------|
| A: Inserter pattern (predicate-based) | Rejected | Depends on internal implementation details |
| B: Full manual construction | Already exists | But tedious to repeat default config |
| C: Index-based insert | Rejected | Fragile — breaks if internal order changes |
| D: Named sources + insertAfter("env") | Rejected | Over-engineering |
| **E: `defaultSources()` method** | **Adopted** | Zero new concepts. Standard List operations |

**Solution:** Expose the default source list as a mutable `ArrayList`:

```java
// 99% of users
PropStack props = new PropStack("myapp");

// 1% who need custom insertion
var sources = PropStack.defaultSources("myapp");
sources.add(2, new VaultPropertySource(client));  // standard List.add
PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

No new API concepts. Java's `List` is the insertion mechanism.

## DD-007: Competitive Analysis — Features Adopted from DGE Review

**DGE Session:** [006-competitive-analysis](../dge/sessions/2026-04-05-006-competitive-analysis.md)
**Context:** DGE session comparing PropStack v0.5.0 against Spring Boot, MicroProfile Config, Typesafe Config, owner, and dotenv. Red Team identified gaps.

**Adopted:**

| Feature | Implementation | Why |
|---------|---------------|-----|
| `TypedKey.stringList()` | Comma-separated → `List<String>` | Spring has it, useful for allowed origins, tags, etc. |
| `TypedKey.secret()` | `sensitive` flag, masked in `dump()` | Prevents password leaks in logs |
| `dump(KeyHolder...)` | Shows all keys with values, defaults, secrets, missing | Spring doesn't have this. One-line diagnostic |
| `trace(key)` | Shows which source each value comes from | Spring can't do this. Ultimate debugging tool |

**Rejected:**

| Feature | Why |
|---------|-----|
| YAML support | Adds dependency (SnakeYAML). `.properties` with dot notation is sufficient |
| Nested structure | Dot notation (`db.host`) already works with flat properties |
| Hot reload | Bug-prone. Restart is safer and simpler |
| IDE metadata JSON | Over-engineering for this scope |

**PropStack advantages over Spring (confirmed by Red Team):**

1. `validate()` — reports ALL missing keys. Spring fails one by one
2. TypedKey enum grouping — feature-based catalog. Spring has nothing equivalent
3. `trace()` — shows exactly which source a value came from. Spring can't
4. `dump()` — one-line diagnostic with secret masking. Spring needs actuator
5. 74 tests, 0 dependencies, <1ms startup

## DD-008: defaultsTo() vs describedAs() — Doc as Code

**DGE Session:** [006-competitive-analysis](../dge/sessions/2026-04-05-006-competitive-analysis.md) (same session as DD-007)
**Problem:** `TypedKey.string("DB_HOST", "localhost")` is ambiguous. Is `"localhost"` a safe production default or a development convenience? If it's a default, `validate()` won't catch missing config. If it's documentation, it shouldn't be returned as a value.

**Solution:** Separate safe defaults from documentation with explicit builder methods:

```java
// Safe default — production-safe, validate() skips
PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587))

// Documentation only — validate() catches, dump() shows description
HOST(TypedKey.string("DB_HOST").describedAs("database hostname"))

// Secret with description
PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("app password"))
```

**dump() output:**
```
SMTP_PORT     = 587 (default)
DB_HOST       = [MISSING] — database hostname
DB_PASSWORD   = [MISSING] — app password (secret)
```

**Old 2-arg factories deprecated.** `TypedKey.string(key, default)` still works but is marked `@Deprecated`.
