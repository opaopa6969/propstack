package org.unlaxer.propstack;

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

    public TypedKey(String key, Class<T> type, T defaultValue) {
        this(key, type, defaultValue, null, false);
    }

    // ---- Builder-style methods ----

    @SuppressWarnings("unchecked")
    public <V> TypedKey<V> defaultsTo(V value) {
        return new TypedKey<>(key, (Class<V>) value.getClass(), value, description, sensitive);
    }

    public TypedKey<T> describedAs(String desc) {
        return new TypedKey<>(key, type, defaultValue, desc, sensitive);
    }

    // ---- Factory methods ----

    public static TypedKey<String> string(String key) {
        return new TypedKey<>(key, String.class, null, null, false);
    }

    public static TypedKey<String> secret(String key) {
        return new TypedKey<>(key, String.class, null, null, true);
    }

    public static TypedKey<Integer> integer(String key) {
        return new TypedKey<>(key, Integer.class, null, null, false);
    }

    public static TypedKey<Boolean> bool(String key) {
        return new TypedKey<>(key, Boolean.class, null, null, false);
    }

    public static TypedKey<Long> longKey(String key) {
        return new TypedKey<>(key, Long.class, null, null, false);
    }

    public static TypedKey<Double> doubleKey(String key) {
        return new TypedKey<>(key, Double.class, null, null, false);
    }

    @SuppressWarnings("unchecked")
    public static TypedKey<List<String>> stringList(String key) {
        return new TypedKey<>(key, (Class<List<String>>) (Class<?>) List.class, null, null, false);
    }
}
