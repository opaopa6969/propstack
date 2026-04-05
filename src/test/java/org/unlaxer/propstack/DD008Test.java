package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DD-008: defaultsTo vs describedAs separation.
 */
class DD008Test {

    enum Smtp implements KeyHolder {
        HOST(TypedKey.string("SMTP_HOST").describedAs("SMTP server hostname")),
        PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
        TLS(TypedKey.bool("SMTP_TLS").defaultsTo(true)),
        USER(TypedKey.string("SMTP_USER")),
        PASSWORD(TypedKey.secret("SMTP_PASSWORD").describedAs("app password, not account password"));

        private final TypedKey<?> key;
        Smtp(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    // ---- defaultsTo ----

    @Test
    void defaultsToReturnsValue() {
        PropStack props = new PropStack(false);
        int port = props.get(Smtp.PORT);
        assertEquals(587, port);
    }

    @Test
    void defaultsToBoolReturnsValue() {
        PropStack props = new PropStack(false);
        boolean tls = props.get(Smtp.TLS);
        assertTrue(tls);
    }

    @Test
    void defaultsToOverriddenBySource() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("SMTP_PORT", "2525"))
        );
        int port = props.get(Smtp.PORT);
        assertEquals(2525, port);
    }

    // ---- describedAs ----

    @Test
    void describedAsHasNoDefault() {
        PropStack props = new PropStack(false);
        String host = props.get(Smtp.HOST);
        assertNull(host);  // describedAs does NOT set a default
    }

    @Test
    void describedAsInDump() {
        PropStack props = new PropStack(false);
        String dump = props.dump(Smtp.class);
        assertTrue(dump.contains("SMTP server hostname"), "Description should appear: " + dump);
        assertTrue(dump.contains("[MISSING] — SMTP server hostname"), dump);
    }

    @Test
    void secretWithDescribedAs() {
        PropStack props = new PropStack(false);
        String dump = props.dump(Smtp.class);
        assertTrue(dump.contains("app password"), "Secret description should appear: " + dump);
    }

    // ---- validate ----

    @Test
    void validateCatchesDescribedAsKeys() {
        PropStack props = new PropStack(false);
        var ex = assertThrows(IllegalStateException.class, () ->
                props.validate(Smtp.class));
        String msg = ex.getMessage();
        assertTrue(msg.contains("SMTP_HOST"), "HOST has no default: " + msg);
        assertTrue(msg.contains("SMTP_USER"), "USER has no default: " + msg);
        assertTrue(msg.contains("SMTP_PASSWORD"), "PASSWORD has no default: " + msg);
        assertFalse(msg.contains("SMTP_PORT"), "PORT has default 587: " + msg);
        assertFalse(msg.contains("SMTP_TLS"), "TLS has default true: " + msg);
    }

    @Test
    void validatePassesWhenAllSet() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("SMTP_HOST", "h", "SMTP_USER", "u", "SMTP_PASSWORD", "p"))
        );
        assertDoesNotThrow(() -> props.validate(Smtp.class));
    }

    // ---- dump full output ----

    @Test
    void dumpFullOutput() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("SMTP_HOST", "smtp.gmail.com", "SMTP_USER", "me", "SMTP_PASSWORD", "s3cret"))
        );
        String dump = props.dump(Smtp.class);

        // Values
        assertTrue(dump.contains("smtp.gmail.com"), dump);
        assertTrue(dump.contains("me"), dump);

        // Defaults
        assertTrue(dump.contains("587 (default)"), dump);
        assertTrue(dump.contains("true (default)"), dump);

        // Secret masked
        assertTrue(dump.contains("******"), dump);
        assertFalse(dump.contains("s3cret"), dump);
    }

    // ---- doc as code: description preserved through chain ----

    @Test
    void describedAsPreservedInDefaultsTo() {
        // Edge case: describedAs + defaultsTo chain
        TypedKey<Integer> key = TypedKey.integer("TIMEOUT").describedAs("request timeout in ms").defaultsTo(5000);
        assertEquals("request timeout in ms", key.description());
        assertEquals(5000, key.defaultValue());
    }
}
