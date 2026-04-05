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
     * Simple: no home directory file.
     * <pre>
     * PropStack props = new PropStack();
     * // Resolution: set() → -D → env → classpath
     * </pre>
     */
    public PropStack() {
        this.sources = new ArrayList<>();
        this.sources.add(first);
        this.sources.add(PropertySource.systemProperties());
        this.sources.add(PropertySource.environmentVariables());
        this.sources.add(PropertySource.fromClasspath("application.properties"));
    }

    /**
     * With app name: also reads from home directory.
     * <pre>
     * PropStack props = new PropStack("myapp");
     * // Resolution: set() → -D → env → ~/.myapp/application.properties → classpath
     * </pre>
     *
     * @param appName your application name. Reads from {@code ~/.<appName>/application.properties}.
     */
    public PropStack(String appName) {
        this();
        if (appName != null && !appName.isEmpty()) {
            // insert home dir source before classpath (last position - 1)
            this.sources.add(this.sources.size() - 1, PropertySource.fromPath(
                    Path.of(System.getProperty("user.home"), "." + appName, "application.properties")));
        }
    }

    /**
     * With command-line args: {@code --KEY=value} has highest priority.
     */
    public PropStack(String appName, String[] args) {
        this(appName);
        this.sources.add(1, PropertySource.fromArgs(args));
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

    /**
     * Returns the default source list as a mutable ArrayList.
     * Use standard List operations to insert/remove/reorder sources,
     * then pass to {@code new PropStack(false, sources.toArray(...))}.
     *
     * <pre>
     * var sources = PropStack.defaultSources("myapp");
     * sources.add(2, vaultSource);  // insert after env vars
     * PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
     * </pre>
     */
    public static java.util.ArrayList<PropertySource> defaultSources(String appName) {
        var list = new java.util.ArrayList<>(List.of(
                PropertySource.systemProperties(),
                PropertySource.environmentVariables()));
        if (appName != null && !appName.isEmpty()) {
            list.add(PropertySource.fromPath(
                    Path.of(System.getProperty("user.home"), "." + appName, "application.properties")));
        }
        list.add(PropertySource.fromClasspath("application.properties"));
        return list;
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

    /**
     * Validate that all required keys (those with no default) have values.
     * Reports ALL missing keys at once.
     *
     * <pre>
     * props.validate(Smtp.class, Db.class);
     * // → IllegalStateException: Missing required properties: [SMTP_HOST, DB_NAME]
     * </pre>
     */
    @SafeVarargs
    public final void validate(Class<? extends KeyHolder>... keyHolderClasses) {
        List<String> missing = new ArrayList<>();
        for (Class<? extends KeyHolder> clazz : keyHolderClasses) {
            if (!clazz.isEnum()) continue;
            for (KeyHolder holder : clazz.getEnumConstants()) {
                TypedKey<?> tk = holder.typedKey();
                if (tk.defaultValue() == null && get(tk.key()).isEmpty()) {
                    missing.add(tk.key());
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required properties: " + missing);
        }
    }

    // ---- dump / trace ----

    /**
     * Dump all keys from the given KeyHolder enums. Shows values, defaults, secrets, and missing keys.
     *
     * <pre>
     * props.dump(Smtp.class, Db.class);
     * // SMTP_HOST     = smtp.gmail.com
     * // SMTP_PORT     = 587 (default)
     * // SMTP_PASSWORD = ****** (secret)
     * // DB_NAME       = [MISSING]
     * </pre>
     */
    @SafeVarargs
    public final String dump(Class<? extends KeyHolder>... keyHolderClasses) {
        StringBuilder sb = new StringBuilder();
        for (Class<? extends KeyHolder> clazz : keyHolderClasses) {
            if (!clazz.isEnum()) continue;
            sb.append("--- ").append(clazz.getSimpleName()).append(" ---\n");
            for (KeyHolder holder : clazz.getEnumConstants()) {
                TypedKey<?> tk = holder.typedKey();
                String raw = get(tk.key()).orElse(null);
                String display;
                if (raw != null) {
                    display = tk.sensitive() ? "******" : raw;
                } else if (tk.defaultValue() != null) {
                    display = String.valueOf(tk.defaultValue()) + " (default)";
                } else {
                    display = "[MISSING]";
                    if (tk.description() != null) {
                        display += " — " + tk.description();
                    }
                }
                sb.append(String.format("  %-25s = %s%n", tk.key(), display));
            }
        }
        return sb.toString();
    }

    /**
     * Trace where a key's value comes from. Returns a description of each source checked.
     *
     * <pre>
     * props.trace("DB_HOST");
     * // DB_HOST:
     * //   [0] set()               → (empty)
     * //   [1] SystemProperties    → (empty)
     * //   [2] EnvironmentVariables → prod-db  ← MATCH
     * //   [3] ~/.volta/app.props  → localhost
     * //   [4] classpath app.props → localhost
     * </pre>
     */
    public String trace(String key) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(":\n");
        for (int i = 0; i < sources.size(); i++) {
            PropertySource source = sources.get(i);
            Optional<String> value = source.getRawValue(key);
            String sourceName = source.name();
            if (sourceName == null || sourceName.contains("$$")) {
                sourceName = "source[" + i + "]";
            }
            String val = value.map(v -> v.isEmpty() ? "(blank)" : v).orElse("(empty)");
            String marker = value.isPresent() ? "  ← MATCH" : "";
            sb.append(String.format("  [%d] %-25s → %s%s%n", i, sourceName, val, marker));
            if (value.isPresent()) break;
        }
        return sb.toString();
    }

    public String trace(PropertyKey key) {
        return trace(key.key());
    }

    public String trace(KeyHolder holder) {
        return trace(holder.typedKey().key());
    }

    // ---- convert ----

    @SuppressWarnings("unchecked")
    private static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        if (type == List.class) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return value;
    }
}
