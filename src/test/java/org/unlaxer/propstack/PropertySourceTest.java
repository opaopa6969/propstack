package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PropertySource} factory methods and default interface methods.
 */
class PropertySourceTest {

    // ---- get(String...) multi-key ----

    @Test
    void getMultiKeyReturnsFirstMatch() {
        PropertySource source = PropertySource.of(Map.of("B", "found"));
        Optional<String> result = source.get("A", "B", "C");
        assertEquals("found", result.orElse(""));
    }

    @Test
    void getMultiKeyReturnsEmptyWhenNoneMatch() {
        PropertySource source = PropertySource.of(Map.of("X", "x"));
        Optional<String> result = source.get("A", "B", "C");
        assertTrue(result.isEmpty());
    }

    @Test
    void getMultiKeyReturnsFirstAvailable() {
        PropertySource source = PropertySource.of(Map.of("A", "first", "B", "second"));
        Optional<String> result = source.get("A", "B");
        assertEquals("first", result.orElse(""));
    }

    // ---- get(PropertyKey...) multi-key ----

    @Test
    void getMultiPropertyKeyReturnsFirstMatch() {
        enum Keys implements PropertyKey {
            ALPHA, BETA;
            public String key() { return name(); }
        }
        PropertySource source = PropertySource.of(Map.of("BETA", "beta-value"));
        Optional<String> result = source.get(Keys.ALPHA, Keys.BETA);
        assertEquals("beta-value", result.orElse(""));
    }

    @Test
    void getMultiPropertyKeyEmptyWhenNoneMatch() {
        enum Keys implements PropertyKey {
            ALPHA, BETA;
            public String key() { return name(); }
        }
        PropertySource source = PropertySource.of(Map.of());
        assertTrue(source.get(Keys.ALPHA, Keys.BETA).isEmpty());
    }

    // ---- toProperties() ----

    @Test
    void toPropertiesContainsAllKeys() {
        PropertySource source = PropertySource.of(Map.of("K1", "V1", "K2", "V2"));
        Properties props = source.toProperties();
        assertEquals("V1", props.getProperty("K1"));
        assertEquals("V2", props.getProperty("K2"));
    }

    @Test
    void toPropertiesFromSystemPropertiesSource() {
        PropertySource source = PropertySource.systemProperties();
        Properties props = source.toProperties();
        // user.home is always present
        assertNotNull(props.getProperty("user.home"));
    }

    // ---- name() ----

    @Test
    void systemPropertiesHasName() {
        PropertySource source = PropertySource.systemProperties();
        assertEquals("SystemProperties", source.name());
    }

    @Test
    void environmentVariablesHasName() {
        PropertySource source = PropertySource.environmentVariables();
        assertEquals("EnvironmentVariables", source.name());
    }

    // ---- forProfile(appName, profile) ----

    @Test
    void forProfileWithAppNameDoesNotThrow() {
        // File won't exist; should return empty source silently
        PropertySource source = PropertySource.forProfile("testapp", "prod");
        assertNotNull(source);
        assertTrue(source.get("ANYTHING").isEmpty());
    }

    // ---- of(Properties) ----

    @Test
    void ofPropertiesReadsValues() {
        Properties p = new Properties();
        p.setProperty("FOO", "bar");
        PropertySource source = PropertySource.of(p);
        assertEquals("bar", source.get("FOO").orElse(""));
    }

    @Test
    void ofPropertiesSet() {
        Properties p = new Properties();
        PropertySource source = PropertySource.of(p);
        source.set("NEW_KEY", "new-value");
        assertEquals("new-value", source.get("NEW_KEY").orElse(""));
    }

    // ---- environmentVariables set() ----

    @Test
    void environmentVariablesSetAndGet() {
        PropertySource envSource = PropertySource.environmentVariables();
        envSource.set("CUSTOM_ENV_TEST", "custom-value");
        assertEquals("custom-value", envSource.get("CUSTOM_ENV_TEST").orElse(""));
    }

    // ---- fromClasspath with missing resource ----

    @Test
    void fromClasspathMissingResourceIsEmpty() {
        PropertySource source = PropertySource.fromClasspath("definitely-not-there-12345.properties");
        assertNotNull(source);
        assertTrue(source.get("ANYTHING").isEmpty());
    }

    // ---- fromArgs edge cases ----

    @Test
    void fromArgsIgnoresNonDashDash() {
        PropertySource source = PropertySource.fromArgs(new String[]{"positional", "KEY=value"});
        assertTrue(source.get("positional").isEmpty());
        assertTrue(source.get("KEY").isEmpty());
    }

    @Test
    void fromArgsHandlesValueWithEquals() {
        PropertySource source = PropertySource.fromArgs(new String[]{"--URL=http://example.com?a=b"});
        assertEquals("http://example.com?a=b", source.get("URL").orElse(""));
    }

    @Test
    void fromArgsEmptyArray() {
        PropertySource source = PropertySource.fromArgs(new String[]{});
        assertTrue(source.get("ANYTHING").isEmpty());
    }
}
