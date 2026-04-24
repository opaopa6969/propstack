package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PropStack convenience methods and traceAll not covered elsewhere:
 * getLong, getDouble, toProperties, traceAll, require(TypedKey/KeyHolder),
 * PropertyKey-based overloads, get(PropertyKey, String).
 */
class PropStackConvenienceTest {

    // ---- getLong / getDouble ----

    @Test
    void getLongParsesCorrectly() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("MY_LONG", "1234567890123"))
        );
        assertEquals(1234567890123L, props.getLong("MY_LONG", 0L));
    }

    @Test
    void getLongReturnsDefaultOnMissing() {
        PropStack props = new PropStack(false);
        assertEquals(9999L, props.getLong("NO_SUCH_LONG", 9999L));
    }

    @Test
    void getLongReturnsDefaultOnBlank() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("BLANK_LONG", "   "))
        );
        assertEquals(42L, props.getLong("BLANK_LONG", 42L));
    }

    @Test
    void getDoubleParsesCorrectly() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("MY_RATE", "3.14"))
        );
        assertEquals(3.14, props.getDouble("MY_RATE", 0.0), 0.001);
    }

    @Test
    void getDoubleReturnsDefaultOnMissing() {
        PropStack props = new PropStack(false);
        assertEquals(1.5, props.getDouble("NO_SUCH_DOUBLE", 1.5), 0.001);
    }

    // ---- toProperties ----

    @Test
    void toPropertiesIncludesSetValues() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("PROP_A", "alpha", "PROP_B", "beta"))
        );
        var p = props.toProperties();
        assertEquals("alpha", p.getProperty("PROP_A"));
        assertEquals("beta", p.getProperty("PROP_B"));
    }

    @Test
    void toPropertiesProgrammaticSetIsIncluded() {
        PropStack props = new PropStack(false);
        props.set("SET_KEY", "set-value");
        var p = props.toProperties();
        assertEquals("set-value", p.getProperty("SET_KEY"));
    }

    // ---- traceAll(String...) ----

    @Test
    void traceAllStringsReturnsList() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("K1", "v1", "K2", "v2"))
        );
        List<String> traces = props.traceAll("K1", "K2", "K3");
        assertEquals(3, traces.size());
        assertTrue(traces.get(0).contains("K1"));
        assertTrue(traces.get(1).contains("K2"));
        assertTrue(traces.get(2).contains("K3"));
    }

    @Test
    void traceAllStringsContainsMatchForFound() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("TRACED_KEY", "traced-val"))
        );
        List<String> traces = props.traceAll("TRACED_KEY");
        assertEquals(1, traces.size());
        assertTrue(traces.get(0).contains("MATCH"));
        assertTrue(traces.get(0).contains("traced-val"));
    }

    // ---- traceAll(Class<? extends KeyHolder>...) ----

    enum TraceGroup implements KeyHolder {
        HOST(TypedKey.string("TRACE_HOST")),
        PORT(TypedKey.integer("TRACE_PORT").defaultsTo(8080));

        private final TypedKey<?> key;
        TraceGroup(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    @Test
    void traceAllKeyHolderClassReturnsList() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("TRACE_HOST", "myhost"))
        );
        List<String> traces = props.traceAll(TraceGroup.class);
        assertEquals(TraceGroup.values().length, traces.size());
    }

    @Test
    void traceAllKeyHolderClassContainsKeyNames() {
        PropStack props = new PropStack(false);
        List<String> traces = props.traceAll(TraceGroup.class);
        assertTrue(traces.get(0).contains("TRACE_HOST"));
        assertTrue(traces.get(1).contains("TRACE_PORT"));
    }

    // ---- trace(KeyHolder) ----

    @Test
    void traceKeyHolderMatchFound() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("TRACE_HOST", "host-value"))
        );
        String trace = props.trace(TraceGroup.HOST);
        assertTrue(trace.contains("TRACE_HOST"));
        assertTrue(trace.contains("MATCH"));
    }

    // ---- require(TypedKey) ----

    enum RequireKeys implements KeyHolder {
        PRESENT(TypedKey.string("RK_PRESENT")),
        DEFAULTED(TypedKey.string("RK_DEFAULT").defaultsTo("the-default")),
        ABSENT(TypedKey.string("RK_ABSENT"));

        private final TypedKey<?> key;
        RequireKeys(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    @Test
    void requireTypedKeyThrowsWhenMissingNoDefault() {
        PropStack props = new PropStack(false);
        TypedKey<String> key = TypedKey.string("RK_ABSENT");
        assertThrows(IllegalStateException.class, () -> props.require(key));
    }

    @Test
    void requireTypedKeyReturnsDefaultWhenSet() {
        PropStack props = new PropStack(false);
        TypedKey<String> key = TypedKey.string("RK_DEFAULT").defaultsTo("the-default");
        String val = props.require(key);
        assertEquals("the-default", val);
    }

    @Test
    void requireTypedKeyReturnsValue() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("RK_PRESENT", "hello"))
        );
        TypedKey<String> key = TypedKey.string("RK_PRESENT");
        assertEquals("hello", props.require(key));
    }

    // ---- require(KeyHolder) ----

    @Test
    void requireKeyHolderThrowsWhenMissing() {
        PropStack props = new PropStack(false);
        assertThrows(IllegalStateException.class,
                () -> props.require(RequireKeys.ABSENT));
    }

    @Test
    void requireKeyHolderReturnsValue() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("RK_PRESENT", "world"))
        );
        String val = props.require(RequireKeys.PRESENT);
        assertEquals("world", val);
    }

    // ---- PropertyKey-based overloads ----

    enum PK implements PropertyKey {
        HOST, PORT, ENABLED, TIMEOUT, RATE;
        public String key() { return name(); }
    }

    @Test
    void getPropertyKeyWithDefault() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("HOST", "myhost"))
        );
        assertEquals("myhost", props.get(PK.HOST, "fallback"));
    }

    @Test
    void getPropertyKeyWithDefaultWhenMissing() {
        PropStack props = new PropStack(false);
        assertEquals("fallback", props.get(PK.HOST, "fallback"));
    }

    @Test
    void getIntPropertyKey() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("PORT", "9090"))
        );
        assertEquals(9090, props.getInt(PK.PORT));
    }

    @Test
    void getIntPropertyKeyZeroDefault() {
        PropStack props = new PropStack(false);
        assertEquals(0, props.getInt(PK.PORT));
    }

    @Test
    void getIntPropertyKeyWithDefault() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("PORT", "9090"))
        );
        assertEquals(9090, props.getInt(PK.PORT, 8080));
    }

    @Test
    void getIntPropertyKeyWithDefaultWhenMissing() {
        PropStack props = new PropStack(false);
        assertEquals(8080, props.getInt(PK.PORT, 8080));
    }

    @Test
    void getBooleanPropertyKeyWithDefault() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("ENABLED", "true"))
        );
        assertTrue(props.getBoolean(PK.ENABLED, false));
    }

    @Test
    void getBooleanPropertyKeyWithDefaultWhenMissing() {
        PropStack props = new PropStack(false);
        assertTrue(props.getBoolean(PK.ENABLED, true));
    }

    @Test
    void getLongPropertyKeyWithDefault() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("TIMEOUT", "60000"))
        );
        assertEquals(60000L, props.getLong(PK.TIMEOUT, 0L));
    }

    @Test
    void getLongPropertyKeyWithDefaultWhenMissing() {
        PropStack props = new PropStack(false);
        assertEquals(30000L, props.getLong(PK.TIMEOUT, 30000L));
    }

    @Test
    void getDoublePropertyKeyWithDefault() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("RATE", "0.99"))
        );
        assertEquals(0.99, props.getDouble(PK.RATE, 0.0), 0.001);
    }

    @Test
    void getDoublePropertyKeyWithDefaultWhenMissing() {
        PropStack props = new PropStack(false);
        assertEquals(1.0, props.getDouble(PK.RATE, 1.0), 0.001);
    }

    @Test
    void requirePropertyKeyThrowsWhenMissing() {
        PropStack props = new PropStack(false);
        assertThrows(IllegalStateException.class, () -> props.require(PK.HOST));
    }

    @Test
    void requirePropertyKeyReturnsValue() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("HOST", "present"))
        );
        assertEquals("present", props.require(PK.HOST));
    }
}
