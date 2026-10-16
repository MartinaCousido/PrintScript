package org.printscript.formatter.config;

import java.util.Map;
import java.util.Optional;
import org.printscript.config.RulesConfiguration;

// Las reglas de formato, tal como las nombra la consigna.
// Todas son opcionales y ninguna tiene un valor por defecto
public record FormatterConfiguration(
        Optional<Boolean> spacingAroundEquals,
        boolean spacingBeforeColon,
        boolean spacingAfterColon,
        boolean spaceSurroundingOperations,
        boolean singleSpaceSeparation,
        boolean lineBreakAfterStatement,
        Optional<Integer> lineBreaksAfterPrintln,
        Optional<Boolean> ifBraceOnSameLine,
        Optional<Integer> indentInsideIf) {

    public static final String SPACING_AROUND_EQUALS_KEY = "enforce-spacing-around-equals";
    public static final String NO_SPACING_AROUND_EQUALS_KEY = "enforce-no-spacing-around-equals";
    public static final String SPACING_BEFORE_COLON_KEY =
            "enforce-spacing-before-colon-in-declaration";
    public static final String SPACING_AFTER_COLON_KEY =
            "enforce-spacing-after-colon-in-declaration";
    public static final String SPACE_SURROUNDING_OPERATIONS_KEY =
            "mandatory-space-surrounding-operations";
    public static final String SINGLE_SPACE_SEPARATION_KEY = "mandatory-single-space-separation";
    public static final String LINE_BREAK_AFTER_STATEMENT_KEY =
            "mandatory-line-break-after-statement";
    public static final String LINE_BREAKS_AFTER_PRINTLN_KEY = "line-breaks-after-println";
    public static final String IF_BRACE_SAME_LINE_KEY = "if-brace-same-line";
    public static final String IF_BRACE_BELOW_LINE_KEY = "if-brace-below-line";
    public static final String INDENT_INSIDE_IF_KEY = "indent-inside-if";

    // nada activado: el archivo sale tal como entró
    public static FormatterConfiguration none() {
        return new FormatterConfiguration(
                Optional.empty(),
                false,
                false,
                false,
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public FormatterConfiguration {
        lineBreaksAfterPrintln.ifPresent(FormatterConfiguration::requireNotNegative);
        indentInsideIf.ifPresent(FormatterConfiguration::requireNotNegative);
    }

    public static FormatterConfiguration from(RulesConfiguration rules) {
        rules.validateTypes(
                Map.ofEntries(
                        Map.entry(SPACING_AROUND_EQUALS_KEY, Boolean.class),
                        Map.entry(NO_SPACING_AROUND_EQUALS_KEY, Boolean.class),
                        Map.entry(SPACING_BEFORE_COLON_KEY, Boolean.class),
                        Map.entry(SPACING_AFTER_COLON_KEY, Boolean.class),
                        Map.entry(SPACE_SURROUNDING_OPERATIONS_KEY, Boolean.class),
                        Map.entry(SINGLE_SPACE_SEPARATION_KEY, Boolean.class),
                        Map.entry(LINE_BREAK_AFTER_STATEMENT_KEY, Boolean.class),
                        Map.entry(LINE_BREAKS_AFTER_PRINTLN_KEY, Integer.class),
                        Map.entry(IF_BRACE_SAME_LINE_KEY, Boolean.class),
                        Map.entry(IF_BRACE_BELOW_LINE_KEY, Boolean.class),
                        Map.entry(INDENT_INSIDE_IF_KEY, Integer.class)));
        return new FormatterConfiguration(
                equalsSpacingOf(rules),
                rules.getBoolean(SPACING_BEFORE_COLON_KEY).orElse(false),
                rules.getBoolean(SPACING_AFTER_COLON_KEY).orElse(false),
                rules.getBoolean(SPACE_SURROUNDING_OPERATIONS_KEY).orElse(false),
                rules.getBoolean(SINGLE_SPACE_SEPARATION_KEY).orElse(false),
                rules.getBoolean(LINE_BREAK_AFTER_STATEMENT_KEY).orElse(false),
                rules.getInt(LINE_BREAKS_AFTER_PRINTLN_KEY),
                bracePlacementOf(rules),
                rules.getInt(INDENT_INSIDE_IF_KEY));
    }

    // dos claves opuestas sobre el mismo espacio: la que pide espacio manda si están las dos
    private static Optional<Boolean> equalsSpacingOf(RulesConfiguration rules) {
        if (rules.getBoolean(SPACING_AROUND_EQUALS_KEY).orElse(false)) {
            return Optional.of(true);
        }
        if (rules.getBoolean(NO_SPACING_AROUND_EQUALS_KEY).orElse(false)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private static Optional<Boolean> bracePlacementOf(RulesConfiguration rules) {
        if (rules.getBoolean(IF_BRACE_SAME_LINE_KEY).orElse(false)) {
            return Optional.of(true);
        }
        if (rules.getBoolean(IF_BRACE_BELOW_LINE_KEY).orElse(false)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    private static void requireNotNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("El valor no puede ser negativo: " + value);
        }
    }
}
