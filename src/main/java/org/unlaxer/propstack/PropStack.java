package org.unlaxer.propstack;

import java.nio.file.Path;
import java.util.*;

/**
 * Cascading property resolver. First match wins.
 *
 * <p>Default resolution order:</p>
 * <ol>
 *   <li>Programmatic {@link #set(String, String)} calls</li>
 *   <li>{@code -D} Java System Properties</li>
 *   <li>Environment variables</li>
 *   <li>{@code ~/.{appName}/application.properties}</li>
 *   <li>Classpath {@code application.properties} (bundled defaults)</li>
 * </ol>
 *
 * <h3>Quick start</h3>
 * <pre>
 * PropStack props = new PropStack();
 * String dbHost = props.get("DB_HOST", "localhost");
 * int port = props.getInt("PORT", 8080);
 * </pre>
 *
 * <h3>Custom app name</h3>
 * <pre>
 * // Reads from ~/.myapp/application.properties
 * PropStack props = new PropStack("myapp");
 * </pre>
 *
 * <h3>Fully custom sources</h3>
 * <pre>
 * PropStack props = new PropStack(true,
 *     PropertySource.fromPath(Path.of("/etc/myapp.conf")),
 *     PropertySource.fromClasspath("defaults.properties")
 * );
 * </pre>
 */
public class PropStack implements PropertySource {

    private final PropertySource first = PropertySource.of(new HashMap<>());
    private final List<PropertySource> sources;

    /**
     * Default: reads from {@code ~/.volta/application.properties} and classpath.
     */
    public PropStack() {
        this("volta");
    }

    /**
     * Custom app name: reads from {@code ~/.{appName}/application.properties} and classpath.
     */
    public PropStack(String appName) {
        this(true,
                PropertySource.fromPath(
                        Path.of(System.getProperty("user.home"), "." + appName, "application.properties")),
                PropertySource.fromClasspath("application.properties")
        );
    }

    /**
     * Fully custom sources.
     *
     * @param enableEnvironments if true, adds System Properties and env vars to the stack
     * @param extras             additional sources (searched after env, lowest priority)
     */
    public PropStack(boolean enableEnvironments, PropertySource... extras) {
        this.sources = new ArrayList<>();
        this.sources.add(first);
        if (enableEnvironments) {
            this.sources.add(PropertySource.systemProperties());
            this.sources.add(PropertySource.environmentVariables());
        }
        this.sources.addAll(List.of(extras));
    }

    @Override
    public Optional<String> getRawValue(String key) {
        for (PropertySource source : sources) {
            Optional<String> value = source.get(key);
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    @Override
    public PropertySource set(String key, String value) {
        first.set(key, value);
        return this;
    }

    @Override
    public Set<String> keys() {
        Set<String> all = new HashSet<>();
        sources.forEach(s -> all.addAll(s.keys()));
        return all;
    }

    @Override
    public Properties toProperties() {
        Properties props = new Properties();
        for (int i = sources.size() - 1; i >= 0; i--) {
            PropertySource source = sources.get(i);
            source.keys().forEach(key -> source.get(key).ifPresent(v -> props.put(key, v)));
        }
        return props;
    }

    // ---- Convenience methods ----

    public String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return get(key).filter(s -> !s.isBlank()).map(Integer::parseInt).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key).filter(s -> !s.isBlank()).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return get(key).filter(s -> !s.isBlank()).map(Long::parseLong).orElse(defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return get(key).filter(s -> !s.isBlank()).map(Double::parseDouble).orElse(defaultValue);
    }

    public String require(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException("Required property missing: " + key));
    }

    // ---- PropertyKey convenience methods ----

    public String get(PropertyKey key, String defaultValue) {
        return get(key.key(), defaultValue);
    }

    public int getInt(PropertyKey key, int defaultValue) {
        return getInt(key.key(), defaultValue);
    }

    public int getInt(PropertyKey key) {
        return getInt(key.key(), 0);
    }

    public boolean getBoolean(PropertyKey key, boolean defaultValue) {
        return getBoolean(key.key(), defaultValue);
    }

    public long getLong(PropertyKey key, long defaultValue) {
        return getLong(key.key(), defaultValue);
    }

    public double getDouble(PropertyKey key, double defaultValue) {
        return getDouble(key.key(), defaultValue);
    }

    public String require(PropertyKey key) {
        return require(key.key());
    }

    // ---- TypedKey / KeyHolder methods ----

    /**
     * Get a typed value from a {@link TypedKey} directly.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(TypedKey<T> key) {
        return get(key.key())
                .filter(s -> !s.isBlank())
                .map(s -> (T) convert(s, key.type()))
                .orElse(key.defaultValue());
    }

    /**
     * Get a typed value from a {@link KeyHolder} (enum entry).
     *
     * <pre>
     * String host = props.get(Smtp.HOST);
     * int port = props.get(Smtp.PORT);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T get(KeyHolder holder) {
        return (T) get(holder.typedKey());
    }

    /**
     * Require a typed value. Throws if missing and no default.
     */
    @SuppressWarnings("unchecked")
    public <T> T require(TypedKey<T> key) {
        T value = get(key);
        if (value == null) {
            throw new IllegalStateException("Required property missing: " + key.key());
        }
        return value;
    }

    /**
     * Require a typed value from a KeyHolder enum.
     */
    @SuppressWarnings("unchecked")
    public <T> T require(KeyHolder holder) {
        return (T) require(holder.typedKey());
    }

    private static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        return value;
    }
}
