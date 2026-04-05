package org.unlaxer.propstack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypedKeyTest {

    enum Smtp implements KeyHolder {
        HOST(TypedKey.string("SMTP_HOST")),
        PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587)),
        USER(TypedKey.string("SMTP_USER")),
        FROM(TypedKey.string("SMTP_FROM").defaultsTo("noreply@example.com")),
        TLS(TypedKey.bool("SMTP_TLS").defaultsTo(true));

        private final TypedKey<?> key;
        Smtp(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    enum Db implements KeyHolder {
        HOST(TypedKey.string("DB_HOST").defaultsTo("localhost")),
        PORT(TypedKey.integer("DB_PORT").defaultsTo(5432)),
        NAME(TypedKey.string("DB_NAME"));

        private final TypedKey<?> key;
        Db(TypedKey<?> key) { this.key = key; }
        public TypedKey<?> typedKey() { return key; }
    }

    @Test
    void stringDefault() {
        PropStack props = new PropStack(false);
        String from = props.get(Smtp.FROM);
        assertEquals("noreply@example.com", from);
    }

    @Test
    void intDefault() {
        PropStack props = new PropStack(false);
        int port = props.get(Smtp.PORT);
        assertEquals(587, port);
    }

    @Test
    void boolDefault() {
        PropStack props = new PropStack(false);
        boolean tls = props.get(Smtp.TLS);
        assertTrue(tls);
    }

    @Test
    void overrideWithSet() {
        PropStack props = new PropStack(false);
        props.set("SMTP_PORT", "2525");
        int port = props.get(Smtp.PORT);
        assertEquals(2525, port);
    }

    @Test
    void requireThrowsWhenMissing() {
        PropStack props = new PropStack(false);
        assertThrows(IllegalStateException.class, () -> {
            String name = props.require(Db.NAME);
        });
    }

    @Test
    void requireReturnsDefault() {
        PropStack props = new PropStack(false);
        String host = props.require(Db.HOST);
        assertEquals("localhost", host);
    }

    @Test
    void enumGrouping() {
        PropStack props = new PropStack(false);
        props.set("SMTP_HOST", "smtp.gmail.com");
        props.set("SMTP_PORT", "465");
        props.set("DB_HOST", "db.example.com");

        // SMTP group
        assertEquals("smtp.gmail.com", props.get(Smtp.HOST));
        assertEquals(465, (int) props.get(Smtp.PORT));

        // DB group — separate enum, separate concern
        assertEquals("db.example.com", props.get(Db.HOST));
        assertEquals(5432, (int) props.get(Db.PORT));
    }

    @Test
    void valuesEnumeration() {
        PropStack props = new PropStack(false);
        props.set("SMTP_HOST", "mail.test");
        props.set("SMTP_PORT", "25");
        props.set("SMTP_USER", "user@test");

        for (Smtp s : Smtp.values()) {
            Object val = props.get(s);
            assertNotNull(val, s.name() + " should have a value or default");
        }
    }

    @Test
    void directTypedKey() {
        PropStack props = new PropStack(false);
        props.set("MY_TIMEOUT", "3000");
        TypedKey<Long> timeout = TypedKey.longKey("MY_TIMEOUT").defaultsTo(5000L);
        assertEquals(3000L, (long) props.get(timeout));
    }

    @Test
    void directTypedKeyDefault() {
        PropStack props = new PropStack(false);
        TypedKey<Double> rate = TypedKey.doubleKey("RATE").defaultsTo(0.05);
        assertEquals(0.05, props.get(rate), 0.001);
    }
}
