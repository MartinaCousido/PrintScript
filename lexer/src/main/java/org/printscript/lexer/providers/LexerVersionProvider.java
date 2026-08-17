package org.printscript.lexer.providers;
//la puerta de entrada al módulo: le pedís "1.0" y te da el provider.
// Si se pide una versión que no existe, falla con un mensaje claro
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
