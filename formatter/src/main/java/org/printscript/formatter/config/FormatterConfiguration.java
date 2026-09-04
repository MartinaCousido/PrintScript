package org.printscript.formatter.config;

import org.printscript.config.RulesConfiguration;

public record FormatterConfiguration(
        boolean spaceBeforeColon,
        boolean spaceAfterColon,
        boolean spaceAroundAssign,
        int blankLinesBeforePrintln,
        int indentSize,
        boolean braceOnSameLine) {

    public static final String SPACE_BEFORE_COLON_KEY =
            "enforce-spacing-before-colon-in-declaration";
    public static final String SPACE_AFTER_COLON_KEY = "enforce-spacing-after-colon-in-declaration";
    public static final String SPACE_AROUND_ASSIGN_KEY = "enforce-spacing-around-equals";
    public static final String BLANK_LINES_BEFORE_PRINTLN_KEY = "line-breaks-before-println";
    public static final String INDENT_SIZE_KEY = "indent-inside-block";
    public static final String BRACE_ON_SAME_LINE_KEY = "brace-on-same-line";

    private static final int DEFAULT_INDENT_SIZE = 4;

    private static final int MAX_BLANK_LINES_BEFORE_PRINTLN = 2;

    // las reglas de 1.0 no dicen nada de bloques: este constructor las deja como estaban
    // y toma los valores por defecto para lo que agrega 1.1
    public FormatterConfiguration(
            boolean spaceBeforeColon,
            boolean spaceAfterColon,
            boolean spaceAroundAssign,
            int blankLinesBeforePrintln) {
        this(
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundAssign,
                blankLinesBeforePrintln,
                DEFAULT_INDENT_SIZE,
                true);
    }

    public FormatterConfiguration {
        if (blankLinesBeforePrintln < 0
                || blankLinesBeforePrintln > MAX_BLANK_LINES_BEFORE_PRINTLN) {
            throw new IllegalArgumentException(
                    "Cantidad de saltos de línea antes de println fuera de rango: "
                            + blankLinesBeforePrintln
                            + ". Admitidos: 0 a "
                            + MAX_BLANK_LINES_BEFORE_PRINTLN);
        }
        if (indentSize < 0) {
            throw new IllegalArgumentException("La sangría no puede ser negativa: " + indentSize);
        }
    }

    public static FormatterConfiguration from(RulesConfiguration rules) {
        return new FormatterConfiguration(
                rules.getBoolean(SPACE_BEFORE_COLON_KEY).orElse(false),
                rules.getBoolean(SPACE_AFTER_COLON_KEY).orElse(false),
                rules.getBoolean(SPACE_AROUND_ASSIGN_KEY).orElse(false),
                rules.getInt(BLANK_LINES_BEFORE_PRINTLN_KEY).orElse(0),
                rules.getInt(INDENT_SIZE_KEY).orElse(DEFAULT_INDENT_SIZE),
                rules.getBoolean(BRACE_ON_SAME_LINE_KEY).orElse(true));
    }
}
