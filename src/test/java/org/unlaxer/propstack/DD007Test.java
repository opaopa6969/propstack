package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DGE competitive analysis features: List, secret, dump, trace.
 */
class DD007Test {

    enum Smtp implements KeyHolder {
        HOST(TypedKey.string("SMTP_HOST")),
        PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
        USER(TypedKey.string("SMTP_USER")),
        PASSWORD(TypedKey.secret("SMTP_PASSWORD")),
        FROM(TypedKey.string("SMTP_FROM").defaultsTo("noreply@example.com"));

        private final TypedKey<?> key;
        Smtp(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    enum App implements KeyHolder {
        ORIGINS(TypedKey.stringList("ALLOWED_ORIGINS")),
        TAGS(TypedKey.stringList("TAGS").defaultsTo(List.of("default")));

        private final TypedKey<?> key;
        App(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    // ---- List<String> ----

    @Test
    void stringListParsesCsv() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("ALLOWED_ORIGINS", "http://localhost,https://example.com, https://test.com"))
        );
        List<String> origins = props.get(App.ORIGINS);
        assertEquals(3, origins.size());
        assertEquals("http://localhost", origins.get(0));
        assertEquals("https://example.com", origins.get(1));
        assertEquals("https://test.com", origins.get(2));
    }

    @Test
    void stringListDefault() {
        PropStack props = new PropStack(false);
        List<String> tags = props.get(App.TAGS);
        assertEquals(List.of("default"), tags);
    }

    @Test
    void stringListNullWhenMissing() {
        PropStack props = new PropStack(false);
        List<String> origins = props.get(App.ORIGINS);
        assertNull(origins);
    }

    // ---- Secret ----

    @Test
    void secretFlagSet() {
        assertTrue(TypedKey.secret("KEY").sensitive());
        assertFalse(TypedKey.string("KEY").sensitive());
    }

    // ---- dump() ----

    @Test
    void dumpShowsValues() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("SMTP_HOST", "smtp.gmail.com", "SMTP_USER", "me", "SMTP_PASSWORD", "s3cret"))
        );
        String dump = props.dump(Smtp.class);
        assertTrue(dump.contains("smtp.gmail.com"), dump);
        assertTrue(dump.contains("587 (default)"), dump);
        assertTrue(dump.contains("******"), "Password should be masked: " + dump);
        assertFalse(dump.contains("s3cret"), "Password value should NOT appear: " + dump);
        assertTrue(dump.contains("noreply@example.com (default)"), dump);
    }

    @Test
    void dumpShowsMissing() {
        PropStack props = new PropStack(false);
        String dump = props.dump(Smtp.class);
        assertTrue(dump.contains("[MISSING]"), dump);
    }

    @Test
    void dumpSectionHeader() {
        PropStack props = new PropStack(false);
        String dump = props.dump(Smtp.class);
        assertTrue(dump.contains("--- Smtp ---"), dump);
    }

    // ---- trace() ----

    @Test
    void traceShowsMatch() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("DB_HOST", "from-map"))
        );
        String trace = props.trace("DB_HOST");
        assertTrue(trace.contains("DB_HOST:"), trace);
        assertTrue(trace.contains("MATCH"), trace);
        assertTrue(trace.contains("from-map"), trace);
    }

    @Test
    void traceShowsEmpty() {
        PropStack props = new PropStack(false);
        String trace = props.trace("NONEXISTENT");
        assertTrue(trace.contains("NONEXISTENT:"), trace);
        assertTrue(trace.contains("(empty)"), trace);
        assertFalse(trace.contains("MATCH"), trace);
    }

    @Test
    void traceWithKeyHolder() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("SMTP_HOST", "traced"))
        );
        String trace = props.trace(Smtp.HOST);
        assertTrue(trace.contains("SMTP_HOST:"), trace);
        assertTrue(trace.contains("MATCH"), trace);
    }

    @Test
    void traceStopsAtFirstMatch() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("KEY", "first")),
                PropertySource.of(Map.of("KEY", "second"))
        );
        String trace = props.trace("KEY");
        // Should stop after first match, not show second source
        long matchCount = trace.lines().filter(l -> l.contains("MATCH")).count();
        assertEquals(1, matchCount, trace);
    }
}
