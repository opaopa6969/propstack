package org.unlaxer.propstack;

/**
 * Interface for enums that hold a {@link TypedKey}.
 *
 * <pre>
 * enum Smtp implements KeyHolder {
 *     HOST(TypedKey.string("SMTP_HOST")),
 *     PORT(TypedKey.integer("SMTP_PORT").defaultsTo(587));
 *
 *     private final TypedKey&lt;?&gt; key;
 *     Smtp(TypedKey&lt;?&gt; key) { this.key = key; }
 *     public TypedKey&lt;?&gt; typedKey() { return key; }
 * }
 * </pre>
 *
 * <h3>Why {@code TypedKey<?>} instead of {@code TypedKey<T>}?</h3>
 * <p>Java enums cannot be generic ({@code enum Foo<T>} is not legal), so a single
 * enum cannot hold both {@code TypedKey<String>} (HOST) and {@code TypedKey<Integer>}
 * (PORT) with a typed return value. The wildcard allows mixed-type keys in one enum.</p>
 *
 * <p>PropStack performs a localized {@code @SuppressWarnings("unchecked")} cast in
 * {@link PropStack#get(KeyHolder)}. The cast is safe as long as the enum constant and
 * its TypedKey are consistent — which is trivially guaranteed when the TypedKey is
 * declared inline at the enum constant (the usual pattern above). Callers never need
 * to cast; type inference at the call site handles it:
 * <pre>
 * String host = props.get(Smtp.HOST);   // inferred as String
 * int    port = props.get(Smtp.PORT);   // inferred as int
 * </pre>
 * </p>
 */
public interface KeyHolder {
    TypedKey<?> typedKey();
}
