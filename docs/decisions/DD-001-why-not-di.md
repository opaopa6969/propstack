# DD-001: Why Not DI?

**Status:** Accepted  
**Date:** 2026-04-05  
**DGE Session:** [002-why-not-di](../../dge/sessions/2026-04-05-002-why-not-di.md)  
**See also:** [README — Why Not DI?](../../README.md#why-not-di--a-dialogue)

---

## Decision

PropStack uses a `Registry` pattern (service locator with test support) rather than a DI framework.

## Context

The original question was: "Should PropStack include a DI container to manage components?" Spring Boot's `@Autowired`, MicroProfile's `@Inject`, and similar frameworks all offer dependency injection. Should PropStack compete in that space?

## Analysis (DGE Dialogue Summary)

The DGE session brought in four characters: Yana (lazy strategist), Imaizumi (questioner), Sengoku (quality guardian), and Dr. House (diagnostician).

**The Service Locator anti-pattern argument:**  
Mark Seemann's 2010 critique of Service Locator had three points:
1. API lies — dependencies don't appear in constructors
2. Testing is hard
3. Errors happen at runtime, not compile time

**The Spring DI counter-argument:**  
Spring's `@Autowired` has exactly the same problems, plus additional ones:
- Field injection hides dependencies from constructors (Seemann's own critique)
- `NoUniqueBeanDefinitionException` is a runtime error
- CGLIB proxies produce call stacks the debugger can't follow
- `@Conditional` makes the same code behave differently per environment

**The Martin Fowler correction:**  
Fowler's 2004 paper presented DI and Service Locator as equal alternatives. The "anti-pattern" label came from blog posts, not the pattern community.

**The key insight:**  
DI the *principle* (inject dependencies, don't hard-code them) is correct. DI *frameworks* are overkill for most apps.

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

## Decision

`Registry` with `put()`, `get()`, and `clear()` provides:
- No proxies, no magic — direct calls, debugger-friendly
- Test support via `put()` (mock injection) and `clear()` (reset)
- Constructor injection still possible — just write it yourself
- Zero dependencies, zero startup time

The assembly is the application's responsibility. `main()` is the DI container.

## Consequences

- Users unfamiliar with the pattern may ask "why no Spring integration?"
- The DI dialogue in README.md is necessary to explain the rationale
- `Registry.clear()` in `@AfterEach` is a required testing discipline

## Alternatives Considered

| Alternative | Why Rejected |
|-------------|-------------|
| Spring integration | Dependency on Spring; defeats zero-dependency goal |
| Guice/Dagger | Adds annotation processing complexity |
| No registry at all | Too minimal; users need application-scoped component management |
