# PropStack

**Stackable property resolver for Java. No DI. No annotations. One line.**

```java
PropStack props = new PropStack();
String dbHost = props.get("DB_HOST", "localhost");
int port = props.getInt("PORT", 8080);
```

That's it.

## Why?

Every config library wants you to buy into a framework:
- Spring Boot → `@Value` + `@Configuration` + DI container
- MicroProfile Config → CDI + `@Inject` + `@ConfigProperty`
- Typesafe Config → HOCON format + Scala ecosystem
- owner → interfaces + annotations + magic proxies

**PropStack has no opinions.** It reads properties. From multiple sources. First match wins.

## Resolution Order

```
1. props.set("KEY", "value")       ← programmatic override (highest)
2. -DKEY=value                      ← JVM system property
3. KEY=value (env var)              ← environment variable
4. ~/.volta/application.properties  ← user home file
5. classpath application.properties ← bundled defaults (lowest)
```

## Usage

### Maven

```xml
<dependency>
    <groupId>org.unlaxer</groupId>
    <artifactId>propstack</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Basic

```java
PropStack props = new PropStack();

// String with default
String host = props.get("DB_HOST", "localhost");

// Typed getters
int port = props.getInt("PORT", 8080);
boolean debug = props.getBoolean("DEBUG", false);
long timeout = props.getLong("TIMEOUT_MS", 5000L);

// Required (throws if missing)
String secret = props.require("JWT_SECRET");
```

### Custom App Name

```java
// Reads from ~/.myapp/application.properties
PropStack props = new PropStack("myapp");
```

### Custom Sources

```java
PropStack props = new PropStack(true,
    PropertySource.fromPath(Path.of("/etc/myapp/config.properties")),
    PropertySource.fromClasspath("defaults.properties")
);
```

### Variable Expansion

```properties
# application.properties
GREETING=hello ${USER}
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

`${VAR}` is resolved from system properties and environment variables.

### Typed Keys (optional)

```java
enum Config implements PropertyKey {
    DB_HOST, DB_PORT, DB_NAME;
    public String key() { return name(); }
}

PropStack props = new PropStack();
String host = props.get(Config.DB_HOST).orElse("localhost");
```

### Backward Compatibility

If your codebase uses `ApplicationProperties`, it still works:

```java
ApplicationProperties props = new ApplicationProperties();
// Same behavior as new PropStack()
```

## How It Works

PropStack is a list of `PropertySource` instances. Each source is checked in order.
First non-empty match wins.

```
PropStack
  ├── [0] in-memory overrides (set() calls)
  ├── [1] System.getProperty()     ← -D flags
  ├── [2] System.getenv()          ← environment
  ├── [3] ~/.volta/app.properties  ← user file
  └── [4] classpath app.properties ← defaults
```

You can add your own `PropertySource` — implement one method:

```java
PropertySource mySource = new PropertySource() {
    public Optional<String> getRawValue(String key) {
        return Optional.ofNullable(myMap.get(key));
    }
    // set() and keys() have sensible defaults
};
```

## Requirements

- Java 21+
- Zero dependencies (test: JUnit 5)

## License

MIT
