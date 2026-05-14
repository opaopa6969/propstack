package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the new instance-based Registry API.
 * No shared state, no @AfterEach clear() needed.
 */
class RegistryInstanceTest {

    enum TestDB implements RegistryKey<String> {
        PROD(String.class), DEV(String.class);
        private final Class<String> type;
        TestDB(Class<String> type) { this.type = type; }
        public Class<String> type() { return type; }
    }

    // ---- Instance isolation ----

    @Test
    void instancesAreIsolated() {
        Registry r1 = new Registry();
        Registry r2 = new Registry();
        r1.put(PropStack.class, new PropStack());
        assertFalse(r2.contains(PropStack.class), "registries must not share state");
    }

    @Test
    void instanceDoesNotPollutGlobal() {
        // Get current global state
        int before = Registry.global().size();
        Registry local = new Registry();
        local.put(String.class, "local-only");
        assertEquals(before, Registry.global().size(), "global must be unaffected");
    }

    // ---- Instance basic API ----

    @Test
    void instancePutAndGet() {
        Registry r = new Registry();
        r.put(String.class, "hello");
        assertEquals("hello", r.get(String.class));
    }

    @Test
    void instanceGetWithSupplier() {
        Registry r = new Registry();
        String result = r.get(String.class, () -> "lazy");
        assertEquals("lazy", result);
        assertSame(result, r.get(String.class, () -> "not-called"));
    }

    @Test
    void instanceNamedKey() {
        Registry r = new Registry();
        r.put(TestDB.PROD, "jdbc:prod");
        r.put(TestDB.DEV, "jdbc:dev");
        assertEquals("jdbc:prod", r.get(TestDB.PROD));
        assertEquals("jdbc:dev", r.get(TestDB.DEV));
    }

    @Test
    void instanceClear() {
        Registry r = new Registry();
        r.put(String.class, "x");
        assertEquals(1, r.size());
        r.clear();
        assertEquals(0, r.size());
    }

    // ---- global() ----

    @Test
    void globalAlwaysReturnsSameInstance() {
        assertSame(Registry.global(), Registry.global());
    }

    @Test
    void singletonsDelegatesToGlobal() {
        // Singletons.put → visible via Registry.global()
        Singletons.put(SampleComponent.class, new SampleComponent("via-singletons"));
        SampleComponent fromGlobal = Registry.global().get(SampleComponent.class);
        assertNotNull(fromGlobal);
        assertEquals("via-singletons", fromGlobal.tag);
        // cleanup
        Singletons.remove(SampleComponent.class);
        assertFalse(Registry.global().contains(SampleComponent.class));
    }

    static final class SampleComponent {
        final String tag;
        SampleComponent() { this("default"); }
        SampleComponent(String tag) { this.tag = tag; }
    }

    // ---- named() ----

    @Test
    void namedReturnsSameInstanceForSameName() {
        Registry a1 = Registry.named("_test_inst_a");
        Registry a2 = Registry.named("_test_inst_a");
        assertSame(a1, a2);
    }

    @Test
    void namedInstancesAreIsolatedFromEachOther() {
        Registry a = Registry.named("_test_inst_x");
        Registry b = Registry.named("_test_inst_y");
        a.put(String.class, "from-a");
        assertFalse(b.contains(String.class));
        // cleanup
        a.clear();
    }
}
