package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VariableExpander}.
 */
class VariableExpanderTest {

    private final VariableExpander expander = VariableExpander.INSTANCE;

    @Test
    void nullInputReturnsNull() {
        assertNull(expander.apply(null));
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertEquals("", expander.apply(""));
    }

    @Test
    void noPlaceholderPassesThrough() {
        assertEquals("hello world", expander.apply("hello world"));
    }

    @Test
    void expandsSystemProperty() {
        System.setProperty("EXPANDER_TEST_KEY", "expanded-value");
        try {
            String result = expander.apply("prefix_${EXPANDER_TEST_KEY}_suffix");
            assertEquals("prefix_expanded-value_suffix", result);
        } finally {
            System.clearProperty("EXPANDER_TEST_KEY");
        }
    }

    @Test
    void unknownPlaceholderLeftAsIs() {
        // A key that definitely doesn't exist in system props or env
        String input = "${DEFINITELY_MISSING_VARIABLE_XYZ_12345}";
        assertEquals(input, expander.apply(input));
    }

    @Test
    void multiplePlaceholdersExpanded() {
        System.setProperty("VAR_A", "foo");
        System.setProperty("VAR_B", "bar");
        try {
            String result = expander.apply("${VAR_A}-${VAR_B}");
            assertEquals("foo-bar", result);
        } finally {
            System.clearProperty("VAR_A");
            System.clearProperty("VAR_B");
        }
    }

    @Test
    void plainTextWithoutPlaceholders() {
        String input = "no braces here";
        assertEquals(input, expander.apply(input));
    }

    @Test
    void singletonInstanceIsNonNull() {
        assertNotNull(VariableExpander.INSTANCE);
    }
}
