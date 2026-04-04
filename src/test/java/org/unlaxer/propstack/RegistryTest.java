package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryTest {

    @AfterEach
    void cleanup() {
        Registry.clear();
    }

    // ---- By class ----

    @Test
    void getByClassCreatesInstance() {
        PropStack props = Registry.get(PropStack.class);
        assertNotNull(props);
    }

    @Test
    void getByClassReturnsSame() {
        PropStack a = Registry.get(PropStack.class);
        PropStack b = Registry.get(PropStack.class);
        assertSame(a, b);
    }

    @Test
    void putByClassOverrides() {
        PropStack custom = new PropStack("test");
        Registry.put(PropStack.class, custom);
        assertSame(custom, Registry.get(PropStack.class));
    }

    @Test
    void getByClassWithSupplier() {
        PropStack props = Registry.get(PropStack.class, () -> new PropStack("custom"));
        assertNotNull(props);
        assertSame(props, Registry.get(PropStack.class));
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
        Registry.put(TestDB.PROD, "jdbc:prod");
        Registry.put(TestDB.DEV, "jdbc:dev");
        assertEquals("jdbc:prod", Registry.get(TestDB.PROD));
        assertEquals("jdbc:dev", Registry.get(TestDB.DEV));
    }

    @Test
    void namedKeyReturnsNullIfMissing() {
        assertNull(Registry.get(TestDB.PROD));
    }

    @Test
    void namedKeyWithSupplier() {
        String url = Registry.get(TestDB.PROD, () -> "jdbc:default");
        assertEquals("jdbc:default", url);
        assertSame(url, Registry.get(TestDB.PROD));
    }

    @Test
    void namedKeyRemove() {
        Registry.put(TestDB.PROD, "jdbc:prod");
        assertTrue(Registry.contains(TestDB.PROD));
        Registry.remove(TestDB.PROD);
        assertFalse(Registry.contains(TestDB.PROD));
    }

    // ---- By string ----

    @Test
    void stringKeyPutAndGet() {
        Registry.put("myService", "hello");
        assertEquals("hello", Registry.get("myService"));
    }

    // ---- Management ----

    @Test
    void clearRemovesAll() {
        Registry.put(TestDB.PROD, "a");
        Registry.get(PropStack.class);
        assertTrue(Registry.size() >= 2);
        Registry.clear();
        assertEquals(0, Registry.size());
    }

    @Test
    void sameTypeMultipleNames() {
        Registry.put(TestDB.PROD, "prod-url");
        Registry.put(TestDB.DEV, "dev-url");
        assertNotEquals(Registry.get(TestDB.PROD), Registry.get(TestDB.DEV));
        assertEquals(2, Registry.size());
    }

    // ---- Singletons backward compat ----

    @Test
    void singletonsAliasWorks() {
        PropStack props = Singletons.get(PropStack.class);
        assertNotNull(props);
        assertSame(props, Registry.get(PropStack.class));
    }
}
