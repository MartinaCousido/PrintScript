package org.printscript.linter.config;

import java.util.Optional;
import org.printscript.config.RulesConfiguration;

public record LinterConfiguration(
        Optional<IdentifierFormat> identifierFormat, boolean printlnOnlySimpleArgument) {

    public static final String IDENTIFIER_FORMAT_KEY = "identifier_format";
    public static final String PRINTLN_ARGUMENT_KEY = "mandatory-variable-or-literal-in-println";

    public static LinterConfiguration from(RulesConfiguration rules) {
        return new LinterConfiguration(
                rules.getString(IDENTIFIER_FORMAT_KEY).map(LinterConfiguration::identifierFormatOf),
                rules.getBoolean(PRINTLN_ARGUMENT_KEY).orElse(false));
    }

    private static IdentifierFormat identifierFormatOf(String value) {
        return IdentifierFormat.fromConfigurationValue(value)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Formato de identificador no soportado: '"
                                                + value
                                                + "'. Soportados: "
                                                + IdentifierFormat.supportedValues()));
    }
}
