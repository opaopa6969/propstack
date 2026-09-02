package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for untested public-API edge cases: boundary conditions, error paths,
 * and regression-prone branches not covered elsewhere.
 */
class EdgeCaseTest {

    @AfterEach
    void cleanUp() {
        System.clearProperty("CIRC_A");
        System.clearProperty("CIRC_B");
    }

    // ---- numeric getters identify invalid values (SPEC §11.4 / Appendix D) ----

    @Test
    void getIntThrowsHelpfulErrorOnNonNumericValue() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("MY_PORT", "abc"))
        );
        var ex = assertThrows(IllegalStateException.class, () -> props.getInt("MY_PORT", 8080));
        assertEquals("Invalid numeric value for MY_PORT: abc", ex.getMessage());
        assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    @Test
    void otherNumericGettersAlsoIdentifyInvalidValues() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("TIMEOUT", "nope", "RATE", "bad"))
        );

        var longException = assertThrows(IllegalStateException.class,
                () -> props.getLong("TIMEOUT", 5000L));
        assertEquals("Invalid numeric value for TIMEOUT: nope", longException.getMessage());
        assertInstanceOf(NumberFormatException.class, longException.getCause());

        var doubleException = assertThrows(IllegalStateException.class,
                () -> props.getDouble("RATE", 1.0));
        assertEquals("Invalid numeric value for RATE: bad", doubleException.getMessage());
        assertInstanceOf(NumberFormatException.class, doubleException.getCause());
    }

    // ---- fromPath with missing file returns empty source (SPEC §11.4: NoSuchFileException is silent) ----

    @Test
    void fromPathMissingFileReturnsEmptySource() {
        PropertySource source = PropertySource.fromPath(
                Path.of("/definitely/does/not/exist/12345.properties"));
        assertNotNull(source);
        assertTrue(source.get("ANYTHING").isEmpty());
        assertTrue(source.keys().isEmpty());
    }

    // ---- toProperties resolves conflicts consistent with first-match-wins ----

    @Test
    void toPropertiesResolvesConflictsByFirstMatchWins() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("KEY", "first")),
                PropertySource.of(Map.of("KEY", "second"))
        );
        assertEquals("first", props.get("KEY").orElse(""));
        assertEquals("first", props.toProperties().getProperty("KEY"));
    }

    // ---- VariableExpander circular reference does not infinite-loop (single-pass expansion) ----

    @Test
    void circularReferenceDoesNotInfiniteLoop() {
        System.setProperty("CIRC_A", "${CIRC_B}");
        System.setProperty("CIRC_B", "${CIRC_A}");
        PropStack props = new PropStack(false);
        props.set("MSG", "${CIRC_A}");
        // Single-pass: ${CIRC_A} → ${CIRC_B}, no re-scan of the result
        assertEquals("${CIRC_B}", props.get("MSG", ""));
    }

    // ---- keys() unions keys across all sources ----

    @Test
    void keysUnionsAcrossSources() {
        PropStack props = new PropStack(false,
                PropertySource.of(Map.of("A", "1", "B", "2")),
                PropertySource.of(Map.of("B", "x", "C", "3"))
        );
        Set<String> keys = props.keys();
        assertTrue(keys.contains("A"));
        assertTrue(keys.contains("B"));
        assertTrue(keys.contains("C"));
        assertEquals(3, keys.size());
    }
}
