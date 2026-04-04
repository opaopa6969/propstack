package org.unlaxer.propstack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A single property source. Multiple sources are stacked in {@link PropStack}.
 *
 * <p>Built-in sources:</p>
 * <ul>
 *   <li>{@link #systemProperties()} — {@code -D} JVM flags</li>
 *   <li>{@link #environmentVariables()} — OS environment</li>
 *   <li>{@link #fromPath(Path)} — file on disk</li>
 *   <li>{@link #fromClasspath(String)} — bundled in JAR</li>
 *   <li>{@link #of(Map)} — in-memory map</li>
 * </ul>
 */
public interface PropertySource {

    Optional<String> getRawValue(String key);

    PropertySource set(String key, String value);

    Set<String> keys();

    /**
     * Get a value with {@code ${VAR}} expansion applied.
     */
    default Optional<String> get(String key) {
        Optional<String> raw = getRawValue(key);
        if (raw.isPresent()) {
            String value = raw.get();
            for (UnaryOperator<String> effector : valueEffectors()) {
                value = effector.apply(value);
                if (value == null) break;
            }
            return Optional.ofNullable(value);
        }
        return Optional.empty();
    }

    /**
     * Try multiple keys, return first match.
     */
    default Optional<String> get(String... keys) {
        for (String key : keys) {
            Optional<String> value = get(key);
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    default Optional<String> get(PropertyKey key) {
        return get(key.key());
    }

    default Optional<String> get(PropertyKey... keys) {
        for (PropertyKey key : keys) {
            Optional<String> value = get(key);
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    default Properties toProperties() {
        Properties props = new Properties();
        keys().forEach(key -> get(key).ifPresent(v -> props.put(key, v)));
        return props;
    }

    default List<UnaryOperator<String>> valueEffectors() {
        return List.of(VariableExpander.INSTANCE);
    }

    default String name() {
        return getClass().getSimpleName();
    }

    // ---- Factory methods ----

    static PropertySource systemProperties() {
        return new PropertySource() {
            @Override public Optional<String> getRawValue(String key) {
                return Optional.ofNullable(System.getProperty(key));
            }
            @Override public PropertySource set(String key, String value) {
                System.setProperty(key, value); return this;
            }
            @Override public Set<String> keys() {
                return System.getProperties().keySet().stream()
                        .filter(o -> o instanceof String).map(String.class::cast)
                        .collect(Collectors.toSet());
            }
            @Override public String name() { return "SystemProperties"; }
        };
    }

    static PropertySource environmentVariables() {
        Map<String, String> overrides = new HashMap<>();
        return new PropertySource() {
            @Override public Optional<String> getRawValue(String key) {
                String v = overrides.get(key);
                return v != null ? Optional.of(v) : Optional.ofNullable(System.getenv(key));
            }
            @Override public PropertySource set(String key, String value) {
                overrides.put(key, value); return this;
            }
            @Override public Set<String> keys() {
                Set<String> all = new HashSet<>(overrides.keySet());
                all.addAll(System.getenv().keySet());
                return all;
            }
            @Override public String name() { return "EnvironmentVariables"; }
        };
    }

    static PropertySource fromPath(Path path) {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        } catch (Exception ignored) {}
        return of(props);
    }

    static PropertySource fromClasspath(String resource) {
        Properties props = new Properties();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        return of(props);
    }

    static PropertySource of(Properties properties) {
        return new PropertySource() {
            @Override public Optional<String> getRawValue(String key) {
                return Optional.ofNullable(properties.getProperty(key));
            }
            @Override public PropertySource set(String key, String value) {
                properties.setProperty(key, value); return this;
            }
            @Override public Set<String> keys() {
                return properties.keySet().stream()
                        .filter(o -> o instanceof String).map(String.class::cast)
                        .collect(Collectors.toSet());
            }
        };
    }

    static PropertySource of(Map<String, String> map) {
        return new PropertySource() {
            @Override public Optional<String> getRawValue(String key) {
                return Optional.ofNullable(map.get(key));
            }
            @Override public PropertySource set(String key, String value) {
                map.put(key, value); return this;
            }
            @Override public Set<String> keys() { return map.keySet(); }
        };
    }
}
