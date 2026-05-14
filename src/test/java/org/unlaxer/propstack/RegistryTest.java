package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryTest {

    @AfterEach
    void cleanup() {
        Registry.global().clear();
    }

    // ---- By class ----

    @Test
    void getByClassCreatesInstance() {
        PropStack props = Registry.global().get(PropStack.class);
        assertNotNull(props);
    }

    @Test
    void getByClassReturnsSame() {
        PropStack a = Registry.global().get(PropStack.class);
        PropStack b = Registry.global().get(PropStack.class);
        assertSame(a, b);
    }

    @Test
    void putByClassOverrides() {
        PropStack custom = new PropStack("test");
        Registry.global().put(PropStack.class, custom);
        assertSame(custom, Registry.global().get(PropStack.class));
    }

    @Test
    void getByClassWithSupplier() {
        PropStack props = Registry.global().get(PropStack.class, () -> new PropStack("custom"));
        assertNotNull(props);
        assertSame(props, Registry.global().get(PropStack.class));
    }

    // ---- By RegistryKey (named) ----

    enum TestDB implements RegistryKey<String> {
        PROD(String.class),
        DEV(String.class);

        private final Class<String> type;
        TestDB(Class<String> type) { this.type = type; }
        public Class<String> type() { return type; }
    }

    @Test
    void namedKeyPutAndGet() {
        Registry.global().put(TestDB.PROD, "jdbc:prod");
        Registry.global().put(TestDB.DEV, "jdbc:dev");
        assertEquals("jdbc:prod", Registry.global().get(TestDB.PROD));
        assertEquals("jdbc:dev", Registry.global().get(TestDB.DEV));
    }

    @Test
    void namedKeyReturnsNullIfMissing() {
        assertNull(Registry.global().get(TestDB.PROD));
    }

    @Test
    void namedKeyWithSupplier() {
        String url = Registry.global().get(TestDB.PROD, () -> "jdbc:default");
        assertEquals("jdbc:default", url);
        assertSame(url, Registry.global().get(TestDB.PROD));
    }

    @Test
    void namedKeyRemove() {
        Registry.global().put(TestDB.PROD, "jdbc:prod");
        assertTrue(Registry.global().contains(TestDB.PROD));
        Registry.global().remove(TestDB.PROD);
        assertFalse(Registry.global().contains(TestDB.PROD));
    }

    // ---- By string ----

    @Test
    void stringKeyPutAndGet() {
        Registry.global().put("myService", "hello");
        assertEquals("hello", Registry.global().get("myService"));
    }

    // ---- Management ----

    @Test
    void clearRemovesAll() {
        Registry.global().put(TestDB.PROD, "a");
        Registry.global().get(PropStack.class);
        assertTrue(Registry.global().size() >= 2);
        Registry.global().clear();
        assertEquals(0, Registry.global().size());
    }

    @Test
    void sameTypeMultipleNames() {
        Registry.global().put(TestDB.PROD, "prod-url");
        Registry.global().put(TestDB.DEV, "dev-url");
        assertNotEquals(Registry.global().get(TestDB.PROD), Registry.global().get(TestDB.DEV));
        assertEquals(2, Registry.global().size());
    }

    // ---- Singletons backward compat ----

    @Test
    void singletonsAliasWorks() {
        PropStack props = Singletons.get(PropStack.class);
        assertNotNull(props);
        assertSame(props, Registry.global().get(PropStack.class));
    }
}
