package org.printscript.formatter.config;

import org.printscript.config.RulesConfiguration;

public record FormatterConfiguration(
        boolean spaceBeforeColon,
        boolean spaceAfterColon,
        boolean spaceAroundAssign,
        int blankLinesBeforePrintln) {

    public static final String SPACE_BEFORE_COLON_KEY =
            "enforce-spacing-before-colon-in-declaration";
    public static final String SPACE_AFTER_COLON_KEY = "enforce-spacing-after-colon-in-declaration";
    public static final String SPACE_AROUND_ASSIGN_KEY = "enforce-spacing-around-equals";
    public static final String BLANK_LINES_BEFORE_PRINTLN_KEY = "line-breaks-before-println";

    private static final int MAX_BLANK_LINES_BEFORE_PRINTLN = 2;

    public FormatterConfiguration {
        if (blankLinesBeforePrintln < 0
                || blankLinesBeforePrintln > MAX_BLANK_LINES_BEFORE_PRINTLN) {
            throw new IllegalArgumentException(
                    "Cantidad de saltos de línea antes de println fuera de rango: "
                            + blankLinesBeforePrintln
                            + ". Admitidos: 0 a "
                            + MAX_BLANK_LINES_BEFORE_PRINTLN);
        }
    }

    public static FormatterConfiguration from(RulesConfiguration rules) {
        return new FormatterConfiguration(
                rules.getBoolean(SPACE_BEFORE_COLON_KEY).orElse(false),
                rules.getBoolean(SPACE_AFTER_COLON_KEY).orElse(false),
                rules.getBoolean(SPACE_AROUND_ASSIGN_KEY).orElse(false),
                rules.getInt(BLANK_LINES_BEFORE_PRINTLN_KEY).orElse(0));
    }
}
