package org.printscript.formatter.config;

public record Separators(FormatterConfiguration configuration) {

    private static final String SPACE = " ";

    public String colon() {
        return pad(":", configuration.spaceBeforeColon(), configuration.spaceAfterColon());
    }

    public String assign() {
        return pad("=", configuration.spaceAroundAssign(), configuration.spaceAroundAssign());
    }

    // la sangría de un nivel de bloque
    public String indentation() {
        return SPACE.repeat(configuration.indentSize());
    }

    // qué separa la condición de la llave que abre el bloque
    public String openingBrace() {
        return configuration.braceOnSameLine() ? " {" : "\n{";
    }

    private static String pad(String symbol, boolean before, boolean after) {
        return (before ? SPACE : "") + symbol + (after ? SPACE : "");
    }
}
