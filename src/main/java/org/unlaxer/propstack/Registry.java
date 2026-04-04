package org.unlaxer.propstack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Application-scoped component registry. Named + typed.
 *
 * <p>Two modes of use:</p>
 *
 * <h3>1. By class (one per type)</h3>
 * <pre>
 * Registry.put(DataSource.class, ds);
 * DataSource ds = Registry.get(DataSource.class);
 * </pre>
 *
 * <h3>2. By RegistryKey (multiple per type, named)</h3>
 * <pre>
 * enum DB implements RegistryKey&lt;DataSource&gt; {
 *     PROD(DataSource.class), DEV(DataSource.class);
 *     // ...
 * }
 * Registry.put(DB.PROD, prodDs);
 * Registry.put(DB.DEV, devDs);
 * DataSource prod = Registry.get(DB.PROD);
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

    private static final Map<String, Object> instances = new ConcurrentHashMap<>();

    private Registry() {}

    // ---- By class ----

    /**
     * Get or create by class. Uses no-arg constructor if not registered.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) instances.computeIfAbsent(clazz.getName(), k -> {
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
    public static <T> T get(Class<T> clazz, Supplier<T> supplier) {
        return (T) instances.computeIfAbsent(clazz.getName(), k -> supplier.get());
    }

    /**
     * Register by class.
     */
    public static <T> void put(Class<T> clazz, T instance) {
        instances.put(clazz.getName(), instance);
    }

    // ---- By RegistryKey (named) ----

    /**
     * Get by named key.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(RegistryKey<T> key) {
        return (T) instances.get(keyName(key));
    }

    /**
     * Get by named key, or create with supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(RegistryKey<T> key, Supplier<T> supplier) {
        return (T) instances.computeIfAbsent(keyName(key), k -> supplier.get());
    }

    /**
     * Register by named key.
     */
    public static <T> void put(RegistryKey<T> key, T instance) {
        instances.put(keyName(key), instance);
    }

    // ---- By string name ----

    /**
     * Get by arbitrary string name.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String name) {
        return (T) instances.get(name);
    }

    /**
     * Get by string name, or create with supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String name, Supplier<T> supplier) {
        return (T) instances.computeIfAbsent(name, k -> supplier.get());
    }

    /**
     * Register by string name.
     */
    public static void put(String name, Object instance) {
        instances.put(name, instance);
    }

    // ---- Management ----

    public static void remove(Class<?> clazz) {
        instances.remove(clazz.getName());
    }

    public static <T> void remove(RegistryKey<T> key) {
        instances.remove(keyName(key));
    }

    public static void remove(String name) {
        instances.remove(name);
    }

    /**
     * Clear all. Call in test @AfterEach.
     */
    public static void clear() {
        instances.clear();
    }

    public static boolean contains(Class<?> clazz) {
        return instances.containsKey(clazz.getName());
    }

    public static <T> boolean contains(RegistryKey<T> key) {
        return instances.containsKey(keyName(key));
    }

    public static int size() {
        return instances.size();
    }

    private static String keyName(RegistryKey<?> key) {
        return key.type().getName() + "#" + key.name();
    }
}
