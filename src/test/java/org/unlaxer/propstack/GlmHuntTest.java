package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for [glm-hunt] issues #17, #18, #19.
 */
class GlmHuntTest {

    @AfterEach
    void cleanUp() {
        System.clearProperty("OUTER");
        System.clearProperty("INNER");
        System.clearProperty("EXPAND_ME");
    }

    // ---- Issue #17: stringList + defaultsTo → ClassCastException ----

    @Test
    void stringListWithDefaultsToDoesNotThrowOnSet() {
        TypedKey<List<String>> key = TypedKey.stringList("KEY").defaultsTo(List.of("default"));
        PropStack props = new PropStack(false);
        props.set("KEY", "a,b");
        List<String> result = props.get(key);
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void stringListWithDefaultsToReturnsDefaultWhenMissing() {
        TypedKey<List<String>> key = TypedKey.stringList("KEY").defaultsTo(List.of("default"));
        PropStack props = new PropStack(false);
        List<String> result = props.get(key);
        assertEquals(List.of("default"), result);
    }

    @Test
    void stringListDefaultsToPreservesListType() {
        TypedKey<List<String>> key = TypedKey.stringList("KEY").defaultsTo(List.of("x"));
        assertEquals(List.class, key.type());
    }

    @Test
    void integerDefaultsToStillWorks() {
        TypedKey<Integer> key = TypedKey.integer("PORT").defaultsTo(587);
        PropStack props = new PropStack(false);
        props.set("PORT", "2525");
        assertEquals(2525, props.get(key));
    }

    @Test
    void describedAsThenDefaultsToPreservesType() {
        TypedKey<Integer> key = TypedKey.integer("TIMEOUT").describedAs("request timeout").defaultsTo(5000);
        assertEquals(Integer.class, key.type());
        assertEquals(5000, key.defaultValue());
    }

    // ---- Issue #18: get(String,String) vs get(TypedKey<String>) blank handling ----

    @Test
    void getWithDefaultFallsBackOnBlank() {
        PropStack props = new PropStack(false);
        props.set("KEY", "   ");
        assertEquals("fallback", props.get("KEY", "fallback"));
    }

    @Test
    void getTypedKeyStringAndGetStringAgreeOnBlank() {
        PropStack props = new PropStack(false);
        props.set("KEY", "   ");
        TypedKey<String> key = TypedKey.string("KEY").defaultsTo("default");
        assertEquals(props.get("KEY", "default"), props.get(key));
    }

    @Test
    void getWithDefaultReturnsNonBlankValue() {
        PropStack props = new PropStack(false);
        props.set("KEY", "value");
        assertEquals("value", props.get("KEY", "fallback"));
    }

    @Test
    void getWithDefaultReturnsEmptyStringAsMissing() {
        PropStack props = new PropStack(false);
        props.set("KEY", "");
        assertEquals("fallback", props.get("KEY", "fallback"));
    }

    // ---- Issue #19: PropStack.getRawValue returns raw (unexpanded) ----

    @Test
    void getRawValueReturnsUnexpandedValue() {
        System.setProperty("INNER", "expanded");
        PropStack props = new PropStack(false);
        props.set("MSG", "${INNER}");
        assertEquals("${INNER}", props.getRawValue("MSG").orElse(""));
    }

    @Test
    void variableExpansionHappensOnce() {
        System.setProperty("OUTER", "${INNER}");
        System.setProperty("INNER", "expanded");
        PropStack props = new PropStack(false);
        props.set("MSG", "${OUTER}");
        assertEquals("${INNER}", props.get("MSG", ""));
    }

    @Test
    void propStackAsPropertySourceReturnsRaw() {
        System.setProperty("INNER", "expanded");
        PropStack inner = new PropStack(false);
        inner.set("MSG", "${INNER}");
        PropStack outer = new PropStack(false, inner);
        assertEquals("${INNER}", outer.getRawValue("MSG").orElse(""));
    }

    @Test
    void singleLevelExpansionStillWorks() {
        System.setProperty("EXPAND_ME", "world");
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("GREETING", "hello ${EXPAND_ME}"))
        );
        assertEquals("hello world", props.get("GREETING", ""));
    }
}
