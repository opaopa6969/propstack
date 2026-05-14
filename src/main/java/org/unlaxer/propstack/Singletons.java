package org.unlaxer.propstack;

import java.util.function.Supplier;

/**
 * Backward-compatible static API for {@link Registry}.
 *
 * <p>All methods delegate to {@link Registry#global()}.
 * Existing code using {@code Singletons.get(MyClass.class)} continues to work.
 * New code should prefer {@code new Registry()} (instance) or {@code Registry.global()}
 * (explicit global intent).</p>
 */
public final class Singletons {

    private Singletons() {}

    public static <T> T get(Class<T> clazz) {
        return Registry.global().get(clazz);
    }

    public static <T> T get(Class<T> clazz, Supplier<T> supplier) {
        return Registry.global().get(clazz, supplier);
    }

    public static <T> void put(Class<T> clazz, T instance) {
        Registry.global().put(clazz, instance);
    }

    public static <T> void remove(Class<T> clazz) {
        Registry.global().remove(clazz);
    }

    public static void clear() {
        Registry.global().clear();
    }
}
