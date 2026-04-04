package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropStackTest {

    @Test
    void systemPropertyOverridesEnv() {
        System.setProperty("TEST_PROP_1", "from-sysprop");
        PropStack props = new PropStack(true);
        assertEquals("from-sysprop", props.get("TEST_PROP_1", "default"));
        System.clearProperty("TEST_PROP_1");
    }

    @Test
    void defaultValueWhenMissing() {
        PropStack props = new PropStack(true);
        assertEquals("fallback", props.get("NONEXISTENT_KEY_XYZ", "fallback"));
    }

    @Test
    void programmaticSetWins() {
        PropStack props = new PropStack(true);
        props.set("MY_KEY", "override");
        assertEquals("override", props.get("MY_KEY", "default"));
    }

    @Test
    void getIntParsesCorrectly() {
        PropStack props = new PropStack(true);
        props.set("MY_PORT", "9090");
        assertEquals(9090, props.getInt("MY_PORT", 8080));
    }

    @Test
    void getIntReturnsDefaultOnMissing() {
        PropStack props = new PropStack(true);
        assertEquals(8080, props.getInt("NO_SUCH_PORT", 8080));
    }

    @Test
    void getBooleanParsesCorrectly() {
        PropStack props = new PropStack(true);
        props.set("ENABLED", "true");
        assertTrue(props.getBoolean("ENABLED", false));
    }

    @Test
    void variableExpansion() {
        System.setProperty("EXPAND_ME", "world");
        PropStack props = new PropStack(true,
                PropertySource.of(Map.of("GREETING", "hello ${EXPAND_ME}"))
        );
        assertEquals("hello world", props.get("GREETING", ""));
        System.clearProperty("EXPAND_ME");
    }

    @Test
    void requireThrowsOnMissing() {
        PropStack props = new PropStack(true);
        assertThrows(IllegalStateException.class, () -> props.require("DEFINITELY_MISSING_KEY"));
    }

    @Test
    void mapSourceWorks() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("A", "1", "B", "2"))
        );
        assertEquals("1", props.get("A", ""));
        assertEquals("2", props.get("B", ""));
    }

    @Test
    void applicationPropertiesCompatibility() {
        ApplicationProperties ap = new ApplicationProperties();
        assertNotNull(ap.get("user.home", "missing"));
    }

    @Test
    void propertyKeyInterface() {
        enum Keys implements PropertyKey {
            DB_HOST;
            public String key() { return name(); }
        }
        PropStack props = new PropStack(true);
        props.set("DB_HOST", "myhost");
        assertEquals("myhost", props.get(Keys.DB_HOST).orElse(""));
    }

    @Test
    void envVarReadable() {
        // PATH should exist on any OS
        PropStack props = new PropStack(true);
        assertTrue(props.get("PATH").isPresent());
    }
}
