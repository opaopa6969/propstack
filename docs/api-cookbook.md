# PropStack API Cookbook

[English](api-cookbook.md) | [Japanese (日本語)](api-cookbook-ja.md)

Practical examples for every PropStack API. Each recipe shows **when to use it** and **how**.

---

## TypedKey — Defining Keys

### `TypedKey.string(key)`

When: A required string property with no safe default.

```java
HOST(TypedKey.string("DB_HOST"))
// props.require(Db.HOST) → throws if missing
// props.validate(Db.class) → caught as missing
```

### `TypedKey.string(key).describedAs(text)`

When: A required string with documentation shown in `dump()` and `validate()` output.

```java
HOST(TypedKey.string("DB_HOST").describedAs("database hostname"))
// dump() → DB_HOST = [MISSING] — database hostname
// validate() → Missing: [DB_HOST]
```

### `TypedKey.integer(key).defaultsTo(n)`

When: An integer with a production-safe default that `validate()` should skip.

```java
PORT(TypedKey.integer("DB_PORT").defaultsTo(5432))
// props.get(Db.PORT) → 5432 if not set in any source
// validate() → not reported missing
// dump() → DB_PORT = 5432 (default)
```

### `TypedKey.secret(key)`

When: A sensitive string that must never appear in logs.

```java
PASSWORD(TypedKey.secret("DB_PASSWORD"))
// dump() → DB_PASSWORD = ****** (secret)
// require() still throws if missing — secret does not imply optional
```

Combine with `.describedAs()`:

```java
PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("app password for the service account"))
// dump() → DB_PASSWORD = [MISSING] — app password for the service account (secret)
```

### `TypedKey.bool(key).defaultsTo(false)`

When: A feature flag with a safe default.

```java
enum Feature implements KeyHolder {
    DARK_MODE(TypedKey.bool("FEATURE_DARK_MODE").defaultsTo(false)),
    BETA_UI(TypedKey.bool("FEATURE_BETA_UI").defaultsTo(false));
    // ...
}

boolean dark = props.get(Feature.DARK_MODE);  // false if not set
```

### `TypedKey.stringList(key)`

When: A comma-separated list of values.

```java
ORIGINS(TypedKey.stringList("ALLOWED_ORIGINS"))

// application.properties:
// ALLOWED_ORIGINS=https://example.com,https://api.example.com

List<String> origins = props.get(Smtp.ORIGINS);
// → ["https://example.com", "https://api.example.com"]
```

Whitespace around commas is trimmed. Empty entries are removed.

### `TypedKey.longKey(key)` / `TypedKey.doubleKey(key)`

```java
TIMEOUT(TypedKey.longKey("TIMEOUT_MS").defaultsTo(5000L))
RATE(TypedKey.doubleKey("SAMPLE_RATE").defaultsTo(0.1))
```

---

## PropStack — Reading Values

### `props.get(KeyHolder)`

When: Reading a value that has a default. Returns the default if missing.

```java
int port = props.get(Smtp.PORT);   // 587 if not set
```

### `props.require(KeyHolder)`

When: Reading a value that must be present. Throws `IllegalStateException` if missing and no default.

```java
String host = props.require(Smtp.HOST);
// → IllegalStateException: Required property missing: SMTP_HOST
```

### `props.get(String, String)`

When: Ad-hoc lookup without a TypedKey definition.

```java
String base = props.get("API_BASE_URL", "https://api.example.com");
```

### `props.getInt(String, int)` / `getBoolean` / `getLong` / `getDouble`

Typed primitives with inline default. Use when you don't need a full KeyHolder enum.

```java
int workers  = props.getInt("THREAD_POOL_SIZE", 4);
boolean mock = props.getBoolean("USE_MOCK_PAYMENT", false);
```

### `props.set(String, String)`

When: Programmatic override — highest priority, overrides everything.

```java
props.set("LOG_LEVEL", "DEBUG");
// Overrides env var, -D flag, and file values
```

---

## PropStack — Validation and Diagnostics

### `props.validate(Class<KeyHolder>...)`

When: At application startup, before any service is created.

```java
props.validate(Db.class, Smtp.class, Auth.class);
// → IllegalStateException: Missing required properties: [DB_HOST, SMTP_HOST, AUTH_SECRET]
```

Reports ALL missing keys at once. Keys with `.defaultsTo()` are skipped. Keys with `.describedAs()` but no value are reported. Call this before `Registry.put(...)` so you fail fast.

### `props.dump(Class<KeyHolder>...)`

When: Debugging a misconfigured environment, or logging startup config.

```java
System.out.print(props.dump(Db.class, Smtp.class));
```

```
--- Db ---
  DB_HOST                   = prod-db.internal
  DB_PORT                   = 5432 (default)
  DB_NAME                   = myapp
  DB_PASSWORD               = ****** (secret)
--- Smtp ---
  SMTP_HOST                 = smtp.gmail.com
  SMTP_PORT                 = 587 (default)
  SMTP_USER                 = alerts@example.com
  SMTP_PASSWORD             = ****** (secret)
  ALLOWED_ORIGINS           = [MISSING] — comma-separated list of allowed origins
```

Safe to log: secrets are masked, missing keys are visible.

### `props.trace(String)` / `trace(KeyHolder)` / `trace(PropertyKey)`

When: A value is coming from an unexpected source, or not being set at all.

```java
System.out.print(props.trace("DB_HOST"));
System.out.print(props.trace(Db.HOST));  // same result
```

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db.internal  ← MATCH
```

> **Warning:** `trace()` stops printing after the first `MATCH`. This is intentional — it reflects the first-match-wins resolution. Sources after the match (home file, classpath) are not shown even if they also contain the key. To see all sources, iterate `PropStack.defaultSources(appName)` manually.

---

## PropStack — Custom Sources

### `PropertySource.fromPath(Path)`

When: Reading config from an absolute path on disk.

```java
PropStack props = new PropStack(true,
    PropertySource.fromPath(Path.of("/etc/myapp/config.properties"))
);
```

> **Warning:** `fromPath()` silently ignores files that do not exist, are unreadable, or have parse errors. A typo in the path produces no error. Use `trace()` to confirm a path source is loading.

### `PropertySource.fromClasspath(String)`

When: Loading a named properties file bundled in the JAR.

```java
PropertySource.fromClasspath("defaults.properties")
```

### `PropertySource.of(Map)`

When: Injecting test values or programmatic overrides as a source.

```java
PropertySource overrides = PropertySource.of(Map.of(
    "DB_HOST", "test-db",
    "DB_PORT", "5433"
));
PropStack props = new PropStack(false, overrides);
```

### `PropertySource.fromArgs(String[])`

When: Accepting `--KEY=value` flags from the command line.

```java
public static void main(String[] args) {
    PropStack props = new PropStack("myapp", args);
    // java -jar app.jar --DB_HOST=custom-host
    // → DB_HOST resolved as "custom-host" (highest priority)
}
```

### `PropertySource.forUser()` / `forHost()` / `forOs()`

When: Loading per-developer, per-host, or per-OS overrides.

```java
PropStack props = new PropStack("myapp",
    PropertySource.forUser(),   // application.user_alice.properties
    PropertySource.forHost(),   // application.host_prod-01.properties
    PropertySource.forOs()      // application.os_linux.properties
);
```

Files that don't exist are silently skipped. Any developer who does not create their override file just gets the shared defaults.

### `PropStack.defaultSources(String)` — Stack Customization

When: Inserting a custom source at a specific position (e.g. Vault, Consul) (DD-006).

```java
var sources = PropStack.defaultSources("myapp");
// sources = [SystemProperties, EnvironmentVariables, ~/.myapp/app.props, classpath app.props]

sources.add(2, new VaultPropertySource(vaultClient));  // after env, before home file

PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
```

Standard `List.add(index, element)` — no new concepts.

---

## Registry — Managing Components

### `Registry.put(Class, T)` / `Registry.get(Class)`

When: One instance per type.

```java
Registry.put(DataSource.class, HikariDataSource.createPool(config));
DataSource ds = Registry.get(DataSource.class);
```

### `Registry.get(Class, Supplier<T>)`

When: Lazy initialization — create on first access.

```java
// Created once on first get(), cached for all subsequent calls
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

### `Registry.put(RegistryKey<T>, T)` / `Registry.get(RegistryKey<T>)`

When: Multiple instances of the same type.

```java
enum DB implements RegistryKey<DataSource> {
    PRIMARY(DataSource.class),
    REPLICA(DataSource.class);

    private final Class<DataSource> type;
    DB(Class<DataSource> type) { this.type = type; }
    public Class<DataSource> type() { return type; }
}

Registry.put(DB.PRIMARY, primaryPool);
Registry.put(DB.REPLICA, replicaPool);

DataSource primary = Registry.get(DB.PRIMARY);
DataSource replica  = Registry.get(DB.REPLICA);
```

### `Registry.put(String, Object)` / `Registry.get(String)`

When: Named lookup without a typed enum.

```java
Registry.put("httpClient", HttpClient.newHttpClient());
HttpClient client = Registry.get("httpClient");
```

### `Registry.contains(Class<?>)` / `Registry.size()`

When: Checking registration state (e.g. in conditional initialization).

```java
if (!Registry.contains(DataSource.class)) {
    Registry.put(DataSource.class, createDataSource(props));
}
```

### `Registry.remove(Class<?>)` / `Registry.clear()`

When: Test cleanup or graceful shutdown.

```java
@AfterEach
void cleanup() {
    Registry.clear();  // reset everything between tests
}

// Targeted removal
Registry.remove(DataSource.class);
```

---

## Variable Expansion

`${VAR}` in property values is expanded automatically.

```properties
# application.properties
GREETING=hello ${USER}
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

```java
String url = props.get("DB_URL", "");
// → "jdbc:postgresql://prod-db:5432/myapp"  (if DB_HOST, DB_PORT, DB_NAME are set)
```

Expansion uses system properties first, then environment variables. Circular references (`A=${B}`, `B=${A}`) are not detected and will produce unexpected results.

---

## Complete Example — Startup Wiring

```java
public class App {

    enum Db implements KeyHolder {
        HOST(TypedKey.string("DB_HOST").describedAs("database hostname")),
        PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
        NAME(TypedKey.string("DB_NAME").describedAs("database schema")),
        PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("database password"));

        private final TypedKey<?> key;
        Db(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    public static void main(String[] args) {
        PropStack props = new PropStack("myapp", args);

        // Fail fast: report all missing keys at once
        props.validate(Db.class);

        // Optional: log configuration summary (secrets masked)
        System.out.print(props.dump(Db.class));

        // Wire components
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://"
            + props.require(Db.HOST) + ":" + props.get(Db.PORT) + "/"
            + props.require(Db.NAME));
        cfg.setPassword(props.require(Db.PASSWORD));
        Registry.put(DataSource.class, new HikariDataSource(cfg));

        Registry.put(UserRepository.class,
            new UserRepository(Registry.get(DataSource.class)));

        // Start
        new AppServer(Registry.get(UserRepository.class)).start();
    }
}
```
