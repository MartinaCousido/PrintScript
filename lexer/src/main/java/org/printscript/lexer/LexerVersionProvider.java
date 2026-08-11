package org.printscript.lexer;

public final class LexerVersionProvider {

    private static final String VERSION_10 = "1.0";

    private LexerVersionProvider() {
    }

    public static LexerProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new LexerProvider10();
        }
        throw new IllegalArgumentException(
            "Versión de PrintScript desconocida: " + version + ". Versiones soportadas: " + VERSION_10);
    }
}
