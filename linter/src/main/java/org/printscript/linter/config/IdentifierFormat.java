package org.printscript.linter.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// el enum con las 3 escrituras de cada uno
public enum IdentifierFormat {
    CAMEL_CASE(List.of("camel case", "camelcase", "camel_case")),
    SNAKE_CASE(List.of("snake case", "snakecase", "snake_case"));

    private final List<String> spellings;

    IdentifierFormat(List<String> spellings) {
        this.spellings = spellings;
    }

    public static Optional<IdentifierFormat> fromConfigurationValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(values())
                .filter(format -> format.spellings.contains(normalized))
                .findFirst();
    }

    public static String supportedValues() {
        return String.join(
                ", ", Arrays.stream(values()).map(format -> format.spellings.get(0)).toList());
    }
}
