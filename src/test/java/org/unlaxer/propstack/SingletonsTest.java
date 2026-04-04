package org.unlaxer.propstack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SingletonsTest {

    @AfterEach
    void cleanup() {
        Singletons.clear();
    }

    @Test
    void getCreatesInstance() {
        PropStack props = Singletons.get(PropStack.class);
        assertNotNull(props);
    }

    @Test
    void getReturnsSameInstance() {
        PropStack a = Singletons.get(PropStack.class);
        PropStack b = Singletons.get(PropStack.class);
        assertSame(a, b);
    }

    @Test
    void putOverridesDefault() {
        PropStack custom = new PropStack("test");
        Singletons.put(PropStack.class, custom);
        assertSame(custom, Singletons.get(PropStack.class));
    }

    @Test
    void getWithSupplier() {
        PropStack props = Singletons.get(PropStack.class, () -> new PropStack("custom"));
        assertNotNull(props);
        assertSame(props, Singletons.get(PropStack.class));
    }

    @Test
    void clearRemovesAll() {
        Singletons.get(PropStack.class);
        Singletons.clear();
        // After clear, a new instance is created
        PropStack fresh = Singletons.get(PropStack.class);
        assertNotNull(fresh);
    }

    @Test
    void removeSpecificClass() {
        PropStack original = Singletons.get(PropStack.class);
        Singletons.remove(PropStack.class);
        PropStack fresh = Singletons.get(PropStack.class);
        assertNotSame(original, fresh);
    }
}
