package org.unlaxer.propstack;

/**
 * Typed, named key for {@link Registry}.
 *
 * <p>Implement on enums to create a compile-time catalog of components:</p>
 * <pre>
 * enum DB implements RegistryKey&lt;DataSource&gt; {
 *     PROD(DataSource.class),
 *     DEV(DataSource.class);
 *
 *     private final Class&lt;DataSource&gt; type;
 *     DB(Class&lt;DataSource&gt; type) { this.type = type; }
 *     public Class&lt;DataSource&gt; type() { return type; }
 * }
 *
 * Registry.put(DB.PROD, prodDs);
 * DataSource ds = Registry.get(DB.PROD);
 * </pre>
 *
 * @param <T> the type of component this key resolves to
 */
public interface RegistryKey<T> {

    /**
     * The name of this key. Defaults to enum name or class simple name.
     */
    String name();

    /**
     * The type of component this key resolves to.
     */
    Class<T> type();
}
