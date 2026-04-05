package org.unlaxer.propstack;

/**
 * A property key with compile-time type information and optional default value.
 *
 * <p>Use the static factory methods to create instances:</p>
 * <pre>
 * TypedKey&lt;String&gt;  host = TypedKey.string("DB_HOST", "localhost");
 * TypedKey&lt;Integer&gt; port = TypedKey.integer("DB_PORT", 5432);
 * TypedKey&lt;Boolean&gt; debug = TypedKey.bool("DEBUG", false);
 * </pre>
 *
 * @param <T> the value type
 */
public record TypedKey<T>(String key, Class<T> type, T defaultValue) {

    // ---- String ----
    public static TypedKey<String> string(String key) {
        return new TypedKey<>(key, String.class, null);
    }

    public static TypedKey<String> string(String key, String defaultValue) {
        return new TypedKey<>(key, String.class, defaultValue);
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
}
