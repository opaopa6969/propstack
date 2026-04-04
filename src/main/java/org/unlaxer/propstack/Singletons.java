package org.unlaxer.propstack;

import java.util.function.Supplier;

/**
 * Backward-compatible alias for {@link Registry}.
 *
 * <p>Existing code using {@code Singletons.get(MyClass.class)} continues to work.
 * New code should prefer {@code Registry.get(...)}.</p>
 */
public final class Singletons {

    private Singletons() {}

    public static <T> T get(Class<T> clazz) {
        return Registry.get(clazz);
    }

    public static <T> T get(Class<T> clazz, Supplier<T> supplier) {
        return Registry.get(clazz, supplier);
    }

    public static <T> void put(Class<T> clazz, T instance) {
        Registry.put(clazz, instance);
    }

    public static <T> void remove(Class<T> clazz) {
        Registry.remove(clazz);
    }

    public static void clear() {
        Registry.clear();
    }
}
