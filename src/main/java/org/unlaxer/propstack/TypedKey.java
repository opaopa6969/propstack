package org.unlaxer.propstack;

import java.util.Arrays;
import java.util.List;

/**
 * A property key with compile-time type information, optional default value,
 * and sensitivity flag for secret masking.
 *
 * <pre>
 * TypedKey&lt;String&gt;  host     = TypedKey.string("DB_HOST", "localhost");
 * TypedKey&lt;Integer&gt; port     = TypedKey.integer("DB_PORT", 5432);
 * TypedKey&lt;String&gt;  password = TypedKey.secret("DB_PASSWORD");
 * TypedKey&lt;List&lt;String&gt;&gt; origins = TypedKey.stringList("ALLOWED_ORIGINS");
 * </pre>
 *
 * @param <T> the value type
 */
public record TypedKey<T>(String key, Class<T> type, T defaultValue, boolean sensitive) {

    public TypedKey(String key, Class<T> type, T defaultValue) {
        this(key, type, defaultValue, false);
    }

    // ---- String ----
    public static TypedKey<String> string(String key) {
        return new TypedKey<>(key, String.class, null);
    }

    public static TypedKey<String> string(String key, String defaultValue) {
        return new TypedKey<>(key, String.class, defaultValue);
    }

    // ---- Secret (String, masked in dump/logs) ----
    public static TypedKey<String> secret(String key) {
        return new TypedKey<>(key, String.class, null, true);
    }

    public static TypedKey<String> secret(String key, String defaultValue) {
        return new TypedKey<>(key, String.class, defaultValue, true);
    }

    // ---- Integer ----
    public static TypedKey<Integer> integer(String key) {
        return new TypedKey<>(key, Integer.class, null);
    }

    public static TypedKey<Integer> integer(String key, int defaultValue) {
        return new TypedKey<>(key, Integer.class, defaultValue);
    }

    // ---- Boolean ----
    public static TypedKey<Boolean> bool(String key) {
        return new TypedKey<>(key, Boolean.class, null);
    }

    public static TypedKey<Boolean> bool(String key, boolean defaultValue) {
        return new TypedKey<>(key, Boolean.class, defaultValue);
    }

    // ---- Long ----
    public static TypedKey<Long> longKey(String key) {
        return new TypedKey<>(key, Long.class, null);
    }

    public static TypedKey<Long> longKey(String key, long defaultValue) {
        return new TypedKey<>(key, Long.class, defaultValue);
    }

    // ---- Double ----
    public static TypedKey<Double> doubleKey(String key) {
        return new TypedKey<>(key, Double.class, null);
    }

    public static TypedKey<Double> doubleKey(String key, double defaultValue) {
        return new TypedKey<>(key, Double.class, defaultValue);
    }

    // ---- List<String> (comma-separated) ----
    @SuppressWarnings("unchecked")
    public static TypedKey<List<String>> stringList(String key) {
        return new TypedKey<>(key, (Class<List<String>>) (Class<?>) List.class, null);
    }

    @SuppressWarnings("unchecked")
    public static TypedKey<List<String>> stringList(String key, List<String> defaultValue) {
        return new TypedKey<>(key, (Class<List<String>>) (Class<?>) List.class, defaultValue);
    }
}
