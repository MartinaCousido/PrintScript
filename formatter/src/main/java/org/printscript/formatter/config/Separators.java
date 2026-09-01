package org.printscript.formatter.config;

public record Separators(FormatterConfiguration configuration) {

    private static final String SPACE = " ";

    public String colon() {
        return pad(":", configuration.spaceBeforeColon(), configuration.spaceAfterColon());
    }

    public String assign() {
        return pad("=", configuration.spaceAroundAssign(), configuration.spaceAroundAssign());
    }

    private static String pad(String symbol, boolean before, boolean after) {
        return (before ? SPACE : "") + symbol + (after ? SPACE : "");
    }
}
