package org.unlaxer.propstack;

/**
 * Interface for enums that hold a {@link TypedKey}.
 *
 * <pre>
 * enum Smtp implements KeyHolder {
 *     HOST(TypedKey.string("SMTP_HOST")),
 *     PORT(TypedKey.integer("SMTP_PORT", 587));
 *
 *     private final TypedKey&lt;?&gt; key;
 *     Smtp(TypedKey&lt;?&gt; key) { this.key = key; }
 *     public TypedKey&lt;?&gt; typedKey() { return key; }
 * }
 * </pre>
 */
public interface KeyHolder {
    TypedKey<?> typedKey();
}
