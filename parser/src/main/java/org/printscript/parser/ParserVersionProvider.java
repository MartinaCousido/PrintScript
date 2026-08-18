package org.printscript.parser;

public final class ParserVersionProvider {

    private static final String VERSION_10 = "1.0";

    private ParserVersionProvider() {
    }

    public static ParserProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new ParserProvider10();
        }
        throw new IllegalArgumentException(
            "Versión de PrintScript desconocida: " + version + ". Versiones soportadas: " + VERSION_10);
    }
}
