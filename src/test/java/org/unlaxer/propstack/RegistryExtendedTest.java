package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryExtendedTest {

    @AfterEach
    void cleanup() {
        Registry.global().clear();
    }

    // ---- remove(Class) ----

    @Test
    void removeByClassRemovesEntry() {
        Registry.global().get(PropStack.class);
        assertTrue(Registry.global().contains(PropStack.class));
        Registry.global().remove(PropStack.class);
        assertFalse(Registry.global().contains(PropStack.class));
    }

    @Test
    void removeByClassAllowsRecreation() {
        PropStack original = Registry.global().get(PropStack.class);
        Registry.global().remove(PropStack.class);
        PropStack fresh = Registry.global().get(PropStack.class);
        assertNotSame(original, fresh);
    }

    // ---- remove(String) ----

    @Test
    void removeByStringRemovesEntry() {
        Registry.global().put("svc", "service-instance");
        assertNotNull(Registry.global().get("svc"));
        Registry.global().remove("svc");
        assertNull(Registry.global().get("svc"));
    }

    // ---- get(String, Supplier) ----

    @Test
    void getByStringWithSupplierCreatesAndCaches() {
        String result = Registry.global().get("dynamic-key", () -> "created");
        assertEquals("created", result);
        String cached = Registry.global().get("dynamic-key", () -> "not-called");
        assertEquals("created", cached);
    }

    @Test
    void getByStringWithSupplierReturnsCachedIfPresent() {
        Registry.global().put("pre-existing", "already-there");
        String result = Registry.global().get("pre-existing", () -> "supplier-never-called");
        assertEquals("already-there", result);
    }

    // ---- contains(Class) ----

    @Test
    void containsClassReturnsFalseWhenNotRegistered() {
        assertFalse(Registry.global().contains(PropStack.class));
    }

    @Test
    void containsClassReturnsTrueAfterGet() {
        Registry.global().get(PropStack.class);
        assertTrue(Registry.global().contains(PropStack.class));
    }

    @Test
    void containsClassReturnsTrueAfterPut() {
        Registry.global().put(PropStack.class, new PropStack("test"));
        assertTrue(Registry.global().contains(PropStack.class));
    }

    // ---- class with no no-arg constructor throws ----

    static class NoDefaultCtor {
        @SuppressWarnings("unused")
        NoDefaultCtor(String required) {}
    }

    @Test
    void getClassWithNoDefaultConstructorThrows() {
        assertThrows(RuntimeException.class, () -> Registry.global().get(NoDefaultCtor.class));
    }

    // ---- size() ----

    enum TestKeys implements RegistryKey<String> {
        FIRST(String.class), SECOND(String.class);
        private final Class<String> type;
        TestKeys(Class<String> type) { this.type = type; }
        public Class<String> type() { return type; }
    }

    @Test
    void sizeReflectsRegistrations() {
        assertEquals(0, Registry.global().size());
        Registry.global().put("a", "1");
        Registry.global().put("b", "2");
        Registry.global().put(TestKeys.FIRST, "x");
        assertEquals(3, Registry.global().size());
    }

    // ---- RegistryKey contains ----

    @Test
    void containsRegistryKeyReturnsFalseWhenMissing() {
        assertFalse(Registry.global().contains(TestKeys.FIRST));
    }

    @Test
    void containsRegistryKeyReturnsTrueAfterPut() {
        Registry.global().put(TestKeys.FIRST, "value");
        assertTrue(Registry.global().contains(TestKeys.FIRST));
        assertFalse(Registry.global().contains(TestKeys.SECOND));
    }
}
