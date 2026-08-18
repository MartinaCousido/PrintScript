package org.printscript.interpreter.providers;

public final class InterpreterVersionProvider {

    private static final String VERSION_10 = "1.0";

    private InterpreterVersionProvider() {
    }

    public static InterpreterProvider providerFor(String version) {
        if (VERSION_10.equals(version)) {
            return new InterpreterProvider10();
        }
        throw new IllegalArgumentException(
            "Versión de PrintScript desconocida: " + version + ". Versiones soportadas: " + VERSION_10);
    }
}
