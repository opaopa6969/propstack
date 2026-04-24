package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Registry methods not covered by RegistryTest:
 * remove(Class), remove(String), get(String, Supplier), contains(Class),
 * and error path when class has no no-arg constructor.
 */
class RegistryExtendedTest {

    @AfterEach
    void cleanup() {
        Registry.clear();
    }

    // ---- remove(Class) ----

    @Test
    void removeByClassRemovesEntry() {
        Registry.get(PropStack.class);
        assertTrue(Registry.contains(PropStack.class));
        Registry.remove(PropStack.class);
        assertFalse(Registry.contains(PropStack.class));
    }

    @Test
    void removeByClassAllowsRecreation() {
        PropStack original = Registry.get(PropStack.class);
        Registry.remove(PropStack.class);
        PropStack fresh = Registry.get(PropStack.class);
        assertNotSame(original, fresh);
    }

    // ---- remove(String) ----

    @Test
    void removeByStringRemovesEntry() {
        Registry.put("svc", "service-instance");
        assertNotNull(Registry.get("svc"));
        Registry.remove("svc");
        assertNull(Registry.get("svc"));
    }

    // ---- get(String, Supplier) ----

    @Test
    void getByStringWithSupplierCreatesAndCaches() {
        String result = Registry.get("dynamic-key", () -> "created");
        assertEquals("created", result);
        // Second call returns cached value
        String cached = Registry.get("dynamic-key", () -> "not-called");
        assertEquals("created", cached);
    }

    @Test
    void getByStringWithSupplierReturnsCachedIfPresent() {
        Registry.put("pre-existing", "already-there");
        String result = Registry.get("pre-existing", () -> "supplier-never-called");
        assertEquals("already-there", result);
    }

    // ---- contains(Class) ----

    @Test
    void containsClassReturnsFalseWhenNotRegistered() {
        assertFalse(Registry.contains(PropStack.class));
    }

    @Test
    void containsClassReturnsTrueAfterGet() {
        Registry.get(PropStack.class);
        assertTrue(Registry.contains(PropStack.class));
    }

    @Test
    void containsClassReturnsTrueAfterPut() {
        Registry.put(PropStack.class, new PropStack("test"));
        assertTrue(Registry.contains(PropStack.class));
    }

    // ---- class with no no-arg constructor throws ----

    static class NoDefaultCtor {
        @SuppressWarnings("unused")
        NoDefaultCtor(String required) {}
    }

    @Test
    void getClassWithNoDefaultConstructorThrows() {
        assertThrows(RuntimeException.class, () -> Registry.get(NoDefaultCtor.class));
    }

    // ---- size() after multiple registrations ----

    enum TestKeys implements RegistryKey<String> {
        FIRST(String.class), SECOND(String.class);
        private final Class<String> type;
        TestKeys(Class<String> type) { this.type = type; }
        public Class<String> type() { return type; }
    }

    @Test
    void sizeReflectsRegistrations() {
        assertEquals(0, Registry.size());
        Registry.put("a", "1");
        Registry.put("b", "2");
        Registry.put(TestKeys.FIRST, "x");
        assertEquals(3, Registry.size());
    }

    // ---- RegistryKey contains ----

    @Test
    void containsRegistryKeyReturnsFalseWhenMissing() {
        assertFalse(Registry.contains(TestKeys.FIRST));
    }

    @Test
    void containsRegistryKeyReturnsTrueAfterPut() {
        Registry.put(TestKeys.FIRST, "value");
        assertTrue(Registry.contains(TestKeys.FIRST));
        assertFalse(Registry.contains(TestKeys.SECOND)); // not put
    }
}
