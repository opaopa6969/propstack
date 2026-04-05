package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DD-005 (CLI args, profile, validate) and DD-006 (defaultSources).
 */
class DD005Test {

    enum Smtp implements KeyHolder {
        HOST(TypedKey.string("SMTP_HOST")),
        PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
        USER(TypedKey.string("SMTP_USER"));

        private final TypedKey<?> key;
        Smtp(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    enum Db implements KeyHolder {
        HOST(TypedKey.string("DB_HOST").defaultsTo("localhost")),
        NAME(TypedKey.string("DB_NAME"));

        private final TypedKey<?> key;
        Db(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    // ---- CLI args ----

    @Test
    void fromArgsParsesKeyValue() {
        var source = PropertySource.fromArgs(new String[]{"--DB_HOST=prod", "--DB_PORT=3306", "ignored"});
        assertEquals("prod", source.get("DB_HOST").orElse(""));
        assertEquals("3306", source.get("DB_PORT").orElse(""));
        assertTrue(source.get("ignored").isEmpty());
    }

    @Test
    void fromArgsHandlesNull() {
        var source = PropertySource.fromArgs(null);
        assertTrue(source.get("anything").isEmpty());
    }

    @Test
    void propStackWithArgs() {
        PropStack props = new PropStack("test", new String[]{"--DB_HOST=from-args"});
        assertEquals("from-args", props.get("DB_HOST", "default"));
    }

    @Test
    void argsOverrideEnvAndFile() {
        System.setProperty("DB_HOST", "from-sysprop");
        PropStack props = new PropStack("test", new String[]{"--DB_HOST=from-args"});
        // args is at index 1 (after set()), sysprop is at index 2
        assertEquals("from-args", props.get("DB_HOST", "default"));
        System.clearProperty("DB_HOST");
    }

    // ---- Profile ----

    @Test
    void forProfileCreatesSource() {
        var source = PropertySource.forProfile("prod");
        assertNotNull(source);
        // No classpath file exists, so empty — but no exception
    }

    @Test
    void forHostCreatesSource() {
        var source = PropertySource.forHost();
        assertNotNull(source);
    }

    @Test
    void forUserCreatesSource() {
        var source = PropertySource.forUser();
        assertNotNull(source);
    }

    @Test
    void forOsCreatesSource() {
        var source = PropertySource.forOs();
        assertNotNull(source);
    }

    // ---- defaultSources ----

    @Test
    void defaultSourcesReturnsMutableList() {
        var sources = PropStack.defaultSources("test");
        assertInstanceOf(ArrayList.class, sources);
        int original = sources.size();
        sources.add(2, PropertySource.of(java.util.Map.of("INJECTED", "yes")));
        assertEquals(original + 1, sources.size());
    }

    @Test
    void defaultSourcesWithInjection() {
        var sources = PropStack.defaultSources("test");
        sources.add(0, PropertySource.of(java.util.Map.of("CUSTOM_KEY", "injected")));
        PropStack props = new PropStack(false, sources.toArray(PropertySource[]::new));
        assertEquals("injected", props.get("CUSTOM_KEY", "nope"));
    }

    // ---- validate ----

    @Test
    void validatePassesWhenAllPresent() {
        PropStack props = new PropStack(false,
                PropertySource.of(java.util.Map.of("SMTP_HOST", "mail", "SMTP_USER", "me", "DB_NAME", "mydb"))
        );
        assertDoesNotThrow(() -> props.validate(Smtp.class, Db.class));
    }

    @Test
    void validateReportsAllMissing() {
        PropStack props = new PropStack(false);
        var ex = assertThrows(IllegalStateException.class, () ->
                props.validate(Smtp.class, Db.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("SMTP_HOST"), "Should report SMTP_HOST: " + msg);
        assertTrue(msg.contains("SMTP_USER"), "Should report SMTP_USER: " + msg);
        assertTrue(msg.contains("DB_NAME"), "Should report DB_NAME: " + msg);
        assertFalse(msg.contains("SMTP_PORT"), "SMTP_PORT has default, should not be reported: " + msg);
        assertFalse(msg.contains("DB_HOST"), "DB_HOST has default, should not be reported: " + msg);
    }

    @Test
    void validateIgnoresKeysWithDefaults() {
        PropStack props = new PropStack(false,
                PropertySource.of(java.util.Map.of("SMTP_HOST", "h", "SMTP_USER", "u", "DB_NAME", "n"))
        );
        // SMTP_PORT (default 587) and DB_HOST (default "localhost") should not be required
        assertDoesNotThrow(() -> props.validate(Smtp.class, Db.class));
    }
}
