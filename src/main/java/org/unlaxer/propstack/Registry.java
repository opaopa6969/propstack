package org.unlaxer.propstack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Application-scoped component registry. Named + typed.
 *
 * <h2>Instance-first design</h2>
 * <p>{@code Registry} is an instantiable class. Each instance has its own isolated
 * component map, which means:</p>
 * <ul>
 *   <li>Parallel tests don't share state — create one {@code Registry} per test</li>
 *   <li>Multiple independent app contexts can coexist in the same JVM</li>
 *   <li>Libraries can use a private Registry without polluting the caller's</li>
 * </ul>
 *
 * <h3>Recommended: inject the registry</h3>
 * <pre>
 * Registry registry = new Registry();
 * registry.put(DataSource.class, createDataSource(props));
 * MyService service = new MyService(registry.get(DataSource.class));
 * </pre>
 *
 * <h3>Global instance (when one process-wide registry is intentional)</h3>
 * <pre>
 * Registry.global().put(DataSource.class, ds);
 * DataSource ds = Registry.global().get(DataSource.class);
 * </pre>
 *
 * <h3>Named registries (module-level scoping)</h3>
 * <pre>
 * Registry app   = Registry.named("app");
 * Registry infra = Registry.named("infra");
 * // Same name always returns the same instance.
 * </pre>
 *
 * <h3>Backward-compatible static API</h3>
 * <p>Code using {@link Singletons} continues to work unchanged — it delegates to
 * {@link #global()}. For direct static access, use {@code Registry.global().put(...)},
 * which makes the intent explicit.</p>
 *
 * <h3>By RegistryKey (multiple instances of the same type)</h3>
 * <pre>
 * enum DB implements RegistryKey&lt;DataSource&gt; {
 *     PROD(DataSource.class), DEV(DataSource.class);
 *     // ...
 * }
 * registry.put(DB.PROD, prodDs);
 * DataSource prod = registry.get(DB.PROD);
 * </pre>
 *
 * <h3>Test support</h3>
 * <pre>
 * // Preferred: fresh instance per test — no clear() needed
 * class MyServiceTest {
 *     private final Registry registry = new Registry();
 *
 *     {@literal @}BeforeEach void setUp() {
 *         registry.put(DataSource.class, mockDataSource);
 *     }
 * }
 *
 * // Also works: shared instance with teardown
 * {@literal @}AfterEach void cleanup() { Registry.global().clear(); }
 * </pre>
 *
 * <h3>Why not DI?</h3>
 * <ul>
 *   <li>0 dependencies, 0ms startup</li>
 *   <li>No proxies, no magic — debugger-friendly</li>
 *   <li>{@code put()} for test mocking, {@code clear()} for reset</li>
 *   <li>Works with constructor injection — they're complementary, not competing</li>
 * </ul>
 */
public final class Registry {

    // ---- Factory ----

    private static final Registry GLOBAL = new Registry();
    private static final ConcurrentHashMap<String, Registry> NAMED = new ConcurrentHashMap<>();

    /**
     * Returns the process-wide global Registry instance.
     * {@link Singletons} delegates here for backward compatibility.
     */
    public static Registry global() {
        return GLOBAL;
    }

    /**
     * Returns a named shared Registry, creating it if absent.
     * Same name always returns the same instance within the same ClassLoader.
     */
    public static Registry named(String name) {
        return NAMED.computeIfAbsent(name, k -> new Registry());
    }

    // ---- Instance ----

    private final Map<String, Object> store = new ConcurrentHashMap<>();

    /** Creates a new, empty, isolated Registry instance. */
    public Registry() {}

    // ---- By class ----

    /**
     * Get or create by class. Uses no-arg constructor if not registered.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        return (T) store.computeIfAbsent(clazz.getName(), k -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate " + clazz.getName(), e);
            }
        });
    }

    /**
     * Get or create by class with custom supplier.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz, Supplier<T> supplier) {
        return (T) store.computeIfAbsent(clazz.getName(), k -> supplier.get());
    }

    /**
     * Register by class.
     */
    public <T> void put(Class<T> clazz, T instance) {
        store.put(clazz.getName(), instance);
    }

    // ---- By RegistryKey (named) ----

    /**
     * Get by named key.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(RegistryKey<T> key) {
        return (T) store.get(keyName(key));
    }

    /**
     * Get by named key, or create with supplier.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(RegistryKey<T> key, Supplier<T> supplier) {
        return (T) store.computeIfAbsent(keyName(key), k -> supplier.get());
    }

    /**
     * Register by named key.
     */
    public <T> void put(RegistryKey<T> key, T instance) {
        store.put(keyName(key), instance);
    }

    // ---- By string name ----

    /**
     * Get by arbitrary string name.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) store.get(name);
    }

    /**
     * Get by string name, or create with supplier.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, Supplier<T> supplier) {
        return (T) store.computeIfAbsent(name, k -> supplier.get());
    }

    /**
     * Register by string name.
     */
    public void put(String name, Object instance) {
        store.put(name, instance);
    }

    // ---- Management ----

    public void remove(Class<?> clazz) {
        store.remove(clazz.getName());
    }

    public <T> void remove(RegistryKey<T> key) {
        store.remove(keyName(key));
    }

    public void remove(String name) {
        store.remove(name);
    }

    /**
     * Clear all entries in this registry.
     *
     * <p>With the instance-per-test pattern, {@code clear()} is usually unnecessary —
     * just create a new {@code Registry()} per test. Use {@code clear()} only
     * when deliberately sharing an instance across tests.</p>
     */
    public void clear() {
        store.clear();
    }

    public boolean contains(Class<?> clazz) {
        return store.containsKey(clazz.getName());
    }

    public <T> boolean contains(RegistryKey<T> key) {
        return store.containsKey(keyName(key));
    }

    public int size() {
        return store.size();
    }

    // ---- Internal ----

    private static String keyName(RegistryKey<?> key) {
        return key.type().getName() + "#" + key.name();
    }
}
