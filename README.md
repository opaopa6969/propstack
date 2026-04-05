# PropStack

[English](README.md) | [Japanese (日本語)](README.ja.md)

**Stackable property resolver + component registry for Java. No DI. No annotations. No proxies.**

```java
enum Db implements KeyHolder {
    HOST(TypedKey.string("DB_HOST").describedAs("database hostname")),
    PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
    NAME(TypedKey.string("DB_NAME")),
    PASSWORD(TypedKey.secret("DB_PASSWORD"));

    private final TypedKey<?> key;
    Db(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}

PropStack props = new PropStack();
props.validate(Db.class);            // reports ALL missing keys at once
String host = props.require(Db.HOST); // type-safe, throws if missing
int port = props.get(Db.PORT);        // 5432 (safe default)
System.out.print(props.dump(Db.class));
// DB_HOST     = prod-db.internal
// DB_PORT     = 5432 (default)
// DB_NAME     = myapp
// DB_PASSWORD = ****** (secret)
```

Type-safe. Doc as code. Secrets masked. Zero dependencies.

## Why?

Every config library wants you to buy into a framework:
- Spring Boot → `@Value` + `@Configuration` + DI container + proxy magic
- MicroProfile Config → CDI + `@Inject` + `@ConfigProperty`
- Typesafe Config → HOCON format + Scala ecosystem
- owner → interfaces + annotations + magic proxies

**PropStack has no opinions.** It reads properties. From multiple sources. First match wins.

It also includes `Registry` — a minimal component registry for people who don't want a DI framework but still need to manage application-scoped components.

## What's Inside

| Class | What it does |
|-------|-------------|
| `PropStack` | Stackable property resolver |
| `Registry` | Named + typed component registry |
| `RegistryKey<T>` | Interface for type-safe catalog enums |
| `TypedKey<T>` | Type-safe property key with `.defaultsTo()`, `.describedAs()`, `.secret()` |
| `KeyHolder` | Interface for enums that hold TypedKey |
| `PropertySource` | Pluggable property source interface |
| `ApplicationProperties` | Backward-compatible alias for PropStack |
| `Singletons` | Backward-compatible alias for Registry |

***

## PropStack — Config

### Resolution Order

```
1. props.set("KEY", "value")       ← programmatic override (highest)
2. -DKEY=value                      ← JVM system property
3. KEY=value (env var)              ← environment variable
4. ~/.volta/application.properties  ← user home file
5. classpath application.properties ← bundled defaults (lowest)
```

### Maven

```xml
<dependency>
    <groupId>org.unlaxer</groupId>
    <artifactId>propstack</artifactId>
    <version>0.7.0</version>
</dependency>
```

### Basic

```java
PropStack props = new PropStack();

String host = props.get("DB_HOST", "localhost");
int port = props.getInt("PORT", 8080);
boolean debug = props.getBoolean("DEBUG", false);
long timeout = props.getLong("TIMEOUT_MS", 5000L);

// Required (throws if missing)
String secret = props.require("JWT_SECRET");
```

### App Name and `~/` Directory

`new PropStack()` with no arguments reads from `~/.volta/application.properties`.
Why "volta"? PropStack was born inside the [volta-platform](https://github.com/opaopa6969/volta-platform) project.

**For your own app, always specify your app name:**

```java
// Reads from ~/.myapp/application.properties
PropStack props = new PropStack("myapp");
```

The pattern is `~/.<appName>/application.properties`. This keeps secrets out of your repo — each developer and server has its own file under their home directory.

### Custom Sources

```java
PropStack props = new PropStack(true,
    PropertySource.fromPath(Path.of("/etc/myapp/config.properties")),
    PropertySource.fromClasspath("defaults.properties")
);
```

### Per-Developer Environment Overrides

Team members have different local environments. Instead of each person editing shared config files, each developer creates their own override file:

```java
PropStack props = new PropStack("myapp",
    PropertySource.forUser(),    // application.user_{username}.properties
    PropertySource.forHost()     // application.host_{hostname}.properties
);
```

```
classpath:
  application.properties                         ← shared defaults
  application.user_alice.properties              ← Alice's overrides
  application.user_bob.properties                ← Bob's overrides
  application.host_prod-server-01.properties     ← production host
```

```properties
# application.user_alice.properties
# Only the keys Alice needs to override
DB_HOST=alice-local-db
DB_PORT=54321
```

New team member asks: "My environment has a different DB host, what do I do?"
Answer: "Create `application.user_{your-name}.properties` and add only the keys you need to change."

No shared files modified. No merge conflicts. Opt-in — you have to explicitly add `PropertySource.forUser()` to enable it.

### Variable Expansion

```properties
# application.properties
GREETING=hello ${USER}
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

`${VAR}` is resolved from system properties and environment variables.

### Typed Keys with KeyHolder

```java
enum Smtp implements KeyHolder {
    HOST(TypedKey.string("SMTP_HOST").describedAs("SMTP server hostname")),
    PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
    USER(TypedKey.string("SMTP_USER")),
    PASSWORD(TypedKey.secret("SMTP_PASSWORD").describedAs("Gmail app password")),
    ORIGINS(TypedKey.stringList("ALLOWED_ORIGINS"));

    private final TypedKey<?> key;
    Smtp(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}

String host = props.require(Smtp.HOST);           // throws if missing
int port = props.get(Smtp.PORT);                   // 587 (safe default)
List<String> origins = props.get(Smtp.ORIGINS);    // comma-separated → List
```

**Key design:**
- `.defaultsTo(value)` — production-safe default (e.g. port 587). `validate()` skips it.
- `.describedAs("text")` — documentation only. `validate()` catches it. `dump()` shows it.
- `.secret()` — masked as `******` in `dump()`.

### validate() — Bulk Validation

```java
props.validate(Smtp.class, Db.class);
// → IllegalStateException: Missing required properties: [SMTP_HOST, SMTP_USER, DB_NAME]
```

Reports ALL missing keys at once. Spring fails one by one.

### dump() — Diagnostic Output

```java
System.out.print(props.dump(Smtp.class));
// --- Smtp ---
//   SMTP_HOST     = smtp.gmail.com
//   SMTP_PORT     = 587 (default)
//   SMTP_USER     = me@gmail.com
//   SMTP_PASSWORD = ****** (secret)
//   ALLOWED_ORIGINS = [MISSING]
```

### trace() — Source Tracking

```java
System.out.print(props.trace("DB_HOST"));
// DB_HOST:
//   [0] set()               → (empty)
//   [1] SystemProperties    → (empty)
//   [2] EnvironmentVariables → prod-db  ← MATCH
```

Shows exactly which source a value comes from. Spring can't do this.

***

## Registry — Components

### By class (one per type)

```java
Registry.put(DataSource.class, dataSource);
DataSource ds = Registry.get(DataSource.class);
```

### By named key (multiple per type)

```java
enum DB implements RegistryKey<DataSource> {
    PROD(DataSource.class),
    DEV(DataSource.class);

    private final Class<DataSource> type;
    DB(Class<DataSource> type) { this.type = type; }
    public Class<DataSource> type() { return type; }
}

Registry.put(DB.PROD, prodDataSource);
Registry.put(DB.DEV, devDataSource);

DataSource prod = Registry.get(DB.PROD);  // type-safe
DataSource dev = Registry.get(DB.DEV);    // same type, different instance
```

### Lazy initialization

```java
// Created on first get(), cached for subsequent calls
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

### Test support

```java
@AfterEach
void cleanup() {
    Registry.clear();  // reset all
}

@Test
void test() {
    Registry.put(DataSource.class, mockDataSource);  // mock injection
    // ...
}
```

***

## Why Not DI? — A Dialogue

> The following is a design dialogue exploring why PropStack chose a Registry over a DI framework.
> This is how we make design decisions — through character-driven debate ([DGE method](https://github.com/opaopa6969/DGE-toolkit)).

---

☕ **Yana** *(lazy strategist)*: First, let's acknowledge: **the DI principle is correct**. Dependencies should be injected, not hard-coded. The question is: do you need a *framework* for that?

👤 **Imaizumi** *(the questioner)*: Why was Service Locator labeled an "anti-pattern" in the first place?

🎩 **Sengoku** *(quality guardian)*: Mark Seemann wrote "Service Locator is an Anti-Pattern" in 2010. Three reasons:
1. The API lies — dependencies don't appear in constructors
2. Testing is hard
3. Errors happen at runtime, not compile time

☕ **Yana**: But here's the thing — **Spring's `@Autowired` has exactly the same problems**:
- Field injection hides dependencies from constructors → Seemann's own critique
- Missing beans throw `NoUniqueBeanDefinitionException` → runtime error
- CGLIB proxies create **call stacks your debugger can't follow**
- `@Conditional` makes **the same code behave differently per environment**

🏥 **Dr. House** *(hidden problem diagnostician)*: Everybody lies. Spring DI has **all the problems of Service Locator, plus proxy black magic on top**.

| Symptom | Service Locator | Spring DI | PropStack Registry |
|---------|----------------|-----------|-------------------|
| Hidden dependencies | `get(X.class)` in method body | `@Autowired` on field | Same |
| Test difficulty | `put()` to swap | `@MockBean` to swap | `put()` to swap |
| Runtime errors | Missing → exception | Missing → startup fail | Missing → exception |
| **Debugging** | **Direct calls** | **Proxy hell** | **Direct calls** |

⚔ **Rivai** *(implementation enforcer)*: The difference is **almost zero**. But Spring DI adds proxy + AOP + implicit scanning as extra complexity.

☕ **Yana**: And here's the key insight: **DI the principle ≠ DI the framework**.

```java
// This IS dependency injection. No framework needed.
class MyService {
    private final DataSource ds;
    MyService(DataSource ds) { this.ds = ds; }  // constructor injection
}

// Who injects? Your choice.
new MyService(Registry.get(DataSource.class));  // Registry
new MyService(prodDs);                          // manual
new MyService(mockDs);                          // test
```

👤 **Imaizumi**: So, to summarize — DI and Service Locator are **complementary, not competing**?

🎩 **Sengoku**: Correct. Martin Fowler's original 2004 paper presented them as **equal alternatives**. The "anti-pattern" label came later, from blog posts, not from the pattern community.

🏥 **Dr. House**: Diagnosis: **"Service Locator is an anti-pattern" is a misdiagnosis.** The real anti-pattern is *"hiding mutable global state without test support."* A Registry with `put()`, `get()`, and `clear()` doesn't have that problem. Vicodin, please.

---

### PropStack's Position

> **The DI principle is right. DI frameworks are overkill for most apps.**
>
> PropStack gives you:
> - `PropStack` — read config from anywhere, no framework
> - `Registry` — manage components by name and type, no framework
> - Constructor injection — **just write it yourself, it's one line**
>
> **The assembly is your responsibility. That's the point.**

```java
// main.java — this is your "DI container". It's 10 lines. You can read it.
PropStack props = new PropStack();
DataSource ds = createDataSource(props);
Registry.put(DataSource.class, ds);

MyService service = new MyService(ds);     // constructor injection
Registry.put(MyService.class, service);    // also in Registry for others

app.start();
```

No scanning. No proxies. No 30-second startup. No magic.
**You wrote it, you can debug it.**

***

## Backward Compatibility

```java
// These still work:
ApplicationProperties props = new ApplicationProperties();
Singletons.get(MyClass.class);
Singletons.put(MyClass.class, instance);
```

## How It Works

```
PropStack
  ├── [0] in-memory overrides (set() calls)
  ├── [1] System.getProperty()     ← -D flags
  ├── [2] System.getenv()          ← environment
  ├── [3] ~/.volta/app.properties  ← user file
  └── [4] classpath app.properties ← defaults

Registry
  └── ConcurrentHashMap<String, Object>
      ├── "com.example.DataSource"              → by class
      ├── "com.example.DataSource#PROD"         → by RegistryKey
      └── "myCustomName"                        → by string
```

## Design Decisions

See [docs/design-decisions.md](docs/design-decisions.md) for the full record of architectural choices, including:
- DD-001: Why not DI?
- DD-002: Naming
- DD-003: TypedKey enum pattern
- DD-004: No object construction in PropStack
- DD-005: Features from fraud-alert
- DD-006: Stack insertion via defaultSources()
- DD-007: Competitive analysis (List, secret, dump, trace)
- DD-008: defaultsTo() vs describedAs() — Doc as Code

## Requirements

- Java 21+
- Zero dependencies (test: JUnit 5)

## License

MIT
