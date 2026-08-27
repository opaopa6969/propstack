# Changelog

All notable changes to PropStack are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Published to Maven Central as `org.unlaxer:propstack`.

## [Unreleased]

### Added
- CI badge in `README.md` and `README.ja.md` (GitHub Actions workflow configured at `.github/workflows/ci.yml`)

### Planned post-1.0
- Javadoc site deployment to GitHub Pages
- JaCoCo coverage integration (target ≥ 80%)
- Thread-safe `set()` on the `first` source
- `trace()` option to keep scanning past the first match

## [1.0.0] - 2026-05-14

### Changed (BREAKING)
- **`Registry` is instance-first.** The class is now instantiable; each `new Registry()` holds an isolated component map. Use `Registry.global()` for the process-wide singleton or `Registry.named("module")` for shared named instances.
- **Static `Registry.put(...)` / `Registry.get(...)` etc. are removed.** Callers must use either an instance (`new Registry()` / `Registry.global()` / `Registry.named(...)`) or the `Singletons` facade, which now delegates to `Registry.global()` for backward compatibility.
- `Registry.clear()` is an instance method; previously-static call sites should migrate to `Registry.global().clear()` or `Singletons.clear()`.

### Migration
- `Registry.put(X.class, x)` → `Singletons.put(X.class, x)` *or* `Registry.global().put(X.class, x)`
- `Registry.get(X.class)` → `Singletons.get(X.class)` *or* `Registry.global().get(X.class)`
- Tests: prefer `Registry registry = new Registry();` per test — no `clear()` needed.

### Added
- `Registry.global()` — process-wide singleton accessor.
- `Registry.named(String)` — named, lazily-initialized shared registries (same name returns the same instance).
- `RegistryInstanceTest` covering instance isolation, `global()`, and `named()` semantics.

### Documentation
- Expanded Javadoc on `KeyHolder` and `PropStack.get(KeyHolder)` explaining why `TypedKey<?>` (enum generics limitation) and why the unchecked cast is safe.
- README "Why PropStack?" section rewritten with a feature comparison table (vs. dotenv-java + enum / SmallRye Config) and a note on SmallRye Config as a valid alternative.
- `spec/SPEC.md` updated to the new instance-first `Registry` API (§3.1.2, §6.1, §7.1.4, §7.2).

## [0.9.1] - 2026-04-19

### Added
- `TypedKey` is now a Java `record` — immutable, `equals`/`hashCode`/`toString` for free
- `TypedKey.bool()` factory for Boolean keys
- `TypedKey.longKey()` / `TypedKey.doubleKey()` factories
- `PropStack.trace(KeyHolder)` and `PropStack.trace(PropertyKey)` overloads
- `PropertySource.forOs()` — loads `application.os_{osname}.properties`
- `PropertySource.fromArgs(String[])` — parses `--KEY=value` CLI arguments
- `PropStack(String appName, String[] args)` constructor for CLI integration
- `Registry.contains(Class<?>)` / `Registry.contains(RegistryKey<T>)` and `Registry.size()`
- `Registry.remove(Class<?>)` / `Registry.remove(RegistryKey<T>)` / `Registry.remove(String)`
- `PropStack.defaultSources(String)` static helper for stack customization (DD-006)

### Changed
- `TypedKey.defaultsTo(V)` / `describedAs(String)` builder methods are now fluent on the record
- `dump()` now appends description to `[MISSING]` lines when `describedAs()` is set
- `trace()` stops printing sources after the first MATCH (first-match-wins)

### Deprecated
- `TypedKey.string(key, default)` two-arg factories — use `.defaultsTo()` instead (DD-008)

## [0.9.0] - 2026-04-10

### Added
- `TypedKey<T>` — type-safe property key with compile-time type information
- `KeyHolder` interface for enums holding `TypedKey` fields (DD-003)
- `RegistryKey<T>` interface for named component lookup
- `PropStack.get(KeyHolder)` / `require(KeyHolder)` typed access
- `PropStack.validate(Class<KeyHolder>...)` — bulk validation reporting all missing keys at once
- `PropStack.dump(Class<KeyHolder>...)` — diagnostic output with secret masking
- `PropStack.trace(String)` — per-key source attribution
- `TypedKey.secret()` — `sensitive` flag, masked as `******` in `dump()` (DD-007)
- `TypedKey.stringList()` — comma-separated string → `List<String>` (DD-007)
- `TypedKey.describedAs(String)` — documentation embedded in key definition (DD-008)
- `TypedKey.defaultsTo(V)` — explicit safe default, skipped by `validate()` (DD-008)
- `PropertySource.forUser()` — `application.user_{username}.properties` per-developer override
- `PropertySource.forHost()` — `application.host_{hostname}.properties` per-host override
- `PropertySource.forProfile(String)` — classpath profile loading
- `VariableExpander` — `${VAR}` resolution from system properties and environment variables

### Changed
- `PropStack` renamed from `ApplicationProperties` (backward-compatible alias retained)
- `Registry` renamed from `Singletons` (backward-compatible alias retained)

## [0.5.0] - 2026-04-05

### Added
- Initial public release
- `ApplicationProperties` — cascading property resolver (set → -D → env → home → classpath)
- `Singletons` — application-scoped component registry backed by `ConcurrentHashMap`
- `PropertySource` interface with `fromPath`, `fromClasspath`, `of(Map)`, `systemProperties()`, `environmentVariables()`
- `PropStack(String appName)` — reads from `~/.<appName>/application.properties`
- `PropertyKey` interface for string-keyed enum patterns
- Zero runtime dependencies

[Unreleased]: https://github.com/opaopa6969/propstack/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/opaopa6969/propstack/compare/v0.9.1...v1.0.0
[0.9.1]: https://github.com/opaopa6969/propstack/compare/v0.9.0...v0.9.1
[0.9.0]: https://github.com/opaopa6969/propstack/compare/v0.5.0...v0.9.0
[0.5.0]: https://github.com/opaopa6969/propstack/releases/tag/v0.5.0
