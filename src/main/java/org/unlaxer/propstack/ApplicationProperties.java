package org.unlaxer.propstack;

/**
 * Backward-compatible alias for {@link PropStack}.
 *
 * <p>Existing code using {@code new ApplicationProperties()} continues to work.
 * New code should prefer {@code new PropStack()} or {@code new PropStack("appName")}.</p>
 */
public class ApplicationProperties extends PropStack {

    /**
     * Default: reads from {@code ~/.volta/application.properties} and classpath.
     */
    public ApplicationProperties() {
        super();
    }

    /**
     * Custom app name.
     */
    public ApplicationProperties(String appName) {
        super(appName);
    }

    /**
     * Fully custom sources.
     */
    public ApplicationProperties(boolean enableEnvironments, PropertySource... extras) {
        super(enableEnvironments, extras);
    }
}
