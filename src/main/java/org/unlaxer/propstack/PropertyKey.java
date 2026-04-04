package org.unlaxer.propstack;

/**
 * Typed key for property lookup.
 * Implement this on enums or constants to get compile-time safety.
 *
 * <pre>
 * enum MyKeys implements PropertyKey {
 *     DB_HOST, DB_PORT;
 *     public String key() { return name(); }
 * }
 * props.get(MyKeys.DB_HOST);
 * </pre>
 */
public interface PropertyKey {
    String key();
}
