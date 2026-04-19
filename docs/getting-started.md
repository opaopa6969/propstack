# Getting Started with PropStack

[English](getting-started.md) | [Japanese (日本語)](getting-started-ja.md)

PropStack is a zero-dependency Java library for reading configuration from multiple sources and managing application-scoped components. This guide gets you from nothing to a working setup in under 5 minutes.

---

## Requirements

- Java 21+
- Maven or Gradle

---

## 1. Add the dependency

### Maven

```xml
<dependency>
    <groupId>org.unlaxer</groupId>
    <artifactId>propstack</artifactId>
    <version>0.9.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'org.unlaxer:propstack:0.9.1'
```

Latest version: [![Maven Central](https://img.shields.io/maven-central/v/org.unlaxer/propstack)](https://central.sonatype.com/artifact/org.unlaxer/propstack)

> Note: CI badge is not yet configured. See [architecture.md — 1.0 Remaining Tasks](architecture.md#10-remaining-tasks).

---

## 2. Basic usage

```java
import org.unlaxer.propstack.PropStack;

PropStack props = new PropStack();

// String with default
String host = props.get("DB_HOST", "localhost");

// Typed primitives
int port     = props.getInt("DB_PORT", 5432);
boolean debug = props.getBoolean("DEBUG", false);
long timeout  = props.getLong("TIMEOUT_MS", 5000L);

// Required — throws IllegalStateException if missing
String secret = props.require("JWT_SECRET");
```

That's it. No annotations, no DI, no config classes.

---

## 3. Add an app name (for secrets)

```java
// Also reads from ~/.myapp/application.properties
PropStack props = new PropStack("myapp");
```

Create `~/.myapp/application.properties` on each machine with the secrets for that environment:

```properties
# ~/.myapp/application.properties  (never committed to git)
DB_PASSWORD=s3cr3t
JWT_SECRET=abc123
```

Shared defaults live in `src/main/resources/application.properties` (committed to git, no secrets).

---

## 4. Define typed keys with KeyHolder

For anything beyond one-off lookups, define your keys as a typed enum:

```java
import org.unlaxer.propstack.*;

enum Db implements KeyHolder {
    HOST(TypedKey.string("DB_HOST").describedAs("database hostname")),
    PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
    NAME(TypedKey.string("DB_NAME").describedAs("database schema name")),
    PASSWORD(TypedKey.secret("DB_PASSWORD").describedAs("database password"));

    private final TypedKey<?> key;
    Db(TypedKey<?> key) { this.key = key; }
    public TypedKey<?> typedKey() { return key; }
}
```

Use the enum with `PropStack`:

```java
PropStack props = new PropStack("myapp");

// Bulk validate at startup — reports ALL missing keys at once
props.validate(Db.class);

// Typed access — no casting required
String host  = props.require(Db.HOST);   // throws if missing, no default
int port     = props.get(Db.PORT);       // 5432 if not set
String name  = props.require(Db.NAME);
String pw    = props.require(Db.PASSWORD);
```

---

## 5. Diagnose your configuration

```java
// Print all keys, values, defaults, secrets, and missing entries
System.out.print(props.dump(Db.class));
```

Sample output:

```
--- Db ---
  DB_HOST                   = prod-db.internal
  DB_PORT                   = 5432 (default)
  DB_NAME                   = myapp
  DB_PASSWORD               = ****** (secret)
```

Find where a specific key came from:

```java
System.out.print(props.trace("DB_HOST"));
```

```
DB_HOST:
  [0] set()               → (empty)
  [1] SystemProperties    → (empty)
  [2] EnvironmentVariables → prod-db.internal  ← MATCH
```

> **Note:** `trace()` stops at the first MATCH. Sources after the match are not shown. This reflects the actual resolution behavior. If you need to see all layers, use `PropStack.defaultSources()` to iterate manually.

---

## 6. Per-developer overrides

Team members have different local environments. Instead of editing shared files, each developer creates their own override file:

```java
PropStack props = new PropStack("myapp",
    PropertySource.forUser()   // application.user_{username}.properties
);
```

Alice creates `src/main/resources/application.user_alice.properties`:

```properties
DB_HOST=alice-local-db
DB_PORT=54321
```

This file can be committed to git — it has no secrets and helps the team. New member onboarding becomes: "Create `application.user_{yourname}.properties` with only the keys you need to override."

---

## 7. Registry — managing components

`Registry` is PropStack's companion for managing application-scoped objects:

```java
import org.unlaxer.propstack.Registry;

// Store
Registry.put(DataSource.class, createDataSource(props));

// Retrieve (thread-safe)
DataSource ds = Registry.get(DataSource.class);

// Lazy initialization
DataSource ds = Registry.get(DataSource.class, () -> createDataSource(props));
```

Wire your application in `main()`:

```java
public static void main(String[] args) {
    PropStack props = new PropStack("myapp");
    props.validate(Db.class);

    Registry.put(DataSource.class, createDataSource(props));
    Registry.put(MyService.class, new MyService(Registry.get(DataSource.class)));

    // start serving
}
```

For test isolation:

```java
@AfterEach
void cleanup() {
    Registry.clear();
}
```

---

## Next steps

- [API Cookbook](api-cookbook.md) — every method with examples
- [Architecture](architecture.md) — how the stack is structured internally
- [Design Decisions](design-decisions.md) — why these choices were made
