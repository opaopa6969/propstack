package org.unlaxer.propstack;

import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${VAR}} placeholders in property values
 * using System properties and environment variables.
 */
public class VariableExpander implements UnaryOperator<String> {

    public static final VariableExpander INSTANCE = new VariableExpander();

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public String apply(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = Optional.ofNullable(System.getProperty(key))
                    .or(() -> Optional.ofNullable(System.getenv(key)))
                    .orElse(matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
