package org.unlaxer.propstack;

import java.util.Arrays;
import java.util.List;

/**
 * A property key with compile-time type information, optional safe default,
 * description, and sensitivity flag.
 *
 * <h3>Safe default (production-safe value)</h3>
 * <pre>
 * PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587))
 * </pre>
 *
 * <h3>Documentation only (no default, validate() catches it)</h3>
 * <pre>
 * HOST(TypedKey.string("DB_HOST").describedAs("database hostname"))
 * </pre>
 *
 * <h3>Secret (masked in dump)</h3>
 * <pre>
 * PASSWORD(TypedKey.secret("DB_PASSWORD"))
 * </pre>
 *
 * <h3>List (comma-separated)</h3>
 * <pre>
 * ORIGINS(TypedKey.stringList("ALLOWED_ORIGINS"))
 * </pre>
 *
 * @param <T> the value type
 */
public record TypedKey<T>(String key, Class<T> type, T defaultValue, String description, boolean sensitive) {

    // ---- Builder-style methods ----

    /**
     * Set a safe default value. Use only for production-safe values (e.g. port 587, timeout 30s).
     * Do NOT use for development conveniences like "localhost".
     */
    @SuppressWarnings("unchecked")
    public <V> TypedKey<V> defaultsTo(V value) {
        return new TypedKey<>(key, (Class<V>) value.getClass(), value, description, sensitive);
    }

    /**
     * Add a description. Shown in dump() output. Does NOT set a default value.
     */
    public TypedKey<T> describedAs(String desc) {
        return new TypedKey<>(key, type, defaultValue, desc, sensitive);
    }

    // ---- Factory methods (starting points) ----

    // String
    public static TypedKey<String> string(String key) {
        return new TypedKey<>(key, String.class, null, null, false);
    }

    /** @deprecated Use {@code string(key).defaultsTo(val)} for safe defaults,
     *  or {@code string(key).describedAs(desc)} for documentation. */
    @Deprecated
    public static TypedKey<String> string(String key, String defaultValue) {
        return new TypedKey<>(key, String.class, defaultValue, null, false);
    }

    // Secret
    public static TypedKey<String> secret(String key) {
        return new TypedKey<>(key, String.class, null, null, true);
    }

    // Integer
    public static TypedKey<Integer> integer(String key) {
        return new TypedKey<>(key, Integer.class, null, null, false);
    }

    /** @deprecated Use {@code integer(key).defaultsTo(val)} */
    @Deprecated
    public static TypedKey<Integer> integer(String key, int defaultValue) {
        return new TypedKey<>(key, Integer.class, defaultValue, null, false);
    }

    // Boolean
    public static TypedKey<Boolean> bool(String key) {
        return new TypedKey<>(key, Boolean.class, null, null, false);
    }

    /** @deprecated Use {@code bool(key).defaultsTo(val)} */
    @Deprecated
    public static TypedKey<Boolean> bool(String key, boolean defaultValue) {
        return new TypedKey<>(key, Boolean.class, defaultValue, null, false);
    }

    // Long
    public static TypedKey<Long> longKey(String key) {
        return new TypedKey<>(key, Long.class, null, null, false);
    }

    /** @deprecated Use {@code longKey(key).defaultsTo(val)} */
    @Deprecated
    public static TypedKey<Long> longKey(String key, long defaultValue) {
        return new TypedKey<>(key, Long.class, defaultValue, null, false);
    }

    // Double
    public static TypedKey<Double> doubleKey(String key) {
        return new TypedKey<>(key, Double.class, null, null, false);
    }

    /** @deprecated Use {@code doubleKey(key).defaultsTo(val)} */
    @Deprecated
    public static TypedKey<Double> doubleKey(String key, double defaultValue) {
        return new TypedKey<>(key, Double.class, defaultValue, null, false);
    }

    // List<String>
    @SuppressWarnings("unchecked")
    public static TypedKey<List<String>> stringList(String key) {
        return new TypedKey<>(key, (Class<List<String>>) (Class<?>) List.class, null, null, false);
    }

    /** @deprecated Use {@code stringList(key).defaultsTo(val)} */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static TypedKey<List<String>> stringList(String key, List<String> defaultValue) {
        return new TypedKey<>(key, (Class<List<String>>) (Class<?>) List.class, defaultValue, null, false);
    }
}
