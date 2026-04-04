package org.unlaxer.propstack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Minimal service locator. No DI framework needed.
 *
 * <p>Get-or-create singletons by class:</p>
 * <pre>
 * PropStack props = Singletons.get(PropStack.class);
 * MyService svc = Singletons.get(MyService.class);
 * </pre>
 *
 * <p>Register explicit instances:</p>
 * <pre>
 * Singletons.put(MyService.class, new MyServiceImpl(config));
 * </pre>
 *
 * <p>Lazy initialization with supplier:</p>
 * <pre>
 * MyService svc = Singletons.get(MyService.class, () -> new MyServiceImpl(config));
 * </pre>
 */
public final class Singletons {

    private static final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    private Singletons() {}

    /**
     * Get or create a singleton by class. Uses no-arg constructor.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) instances.computeIfAbsent(clazz, k -> {
            try {
                return k.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate " + clazz.getName(), e);
            }
        });
    }

    /**
     * Get or create a singleton with a custom supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz, Supplier<T> supplier) {
        return (T) instances.computeIfAbsent(clazz, k -> supplier.get());
    }

    /**
     * Register an explicit instance.
     */
    public static <T> void put(Class<T> clazz, T instance) {
        instances.put(clazz, instance);
    }

    /**
     * Remove a registered instance.
     */
    public static <T> void remove(Class<T> clazz) {
        instances.remove(clazz);
    }

    /**
     * Clear all registered singletons. Useful for testing.
     */
    public static void clear() {
        instances.clear();
    }
}
