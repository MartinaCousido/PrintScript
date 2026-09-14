package org.printscript.cli;

// los códigos de salida que puede devolver el proceso
public enum ExitCode {
    SUCCESS(0), // 0 ok
    SOURCE_ERRORS(1), // 1 errores en el fuente
    USAGE_ERROR(2), // 2 error de uso
    UNSUPPORTED_OPERATION(3); // 3 no soportado

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int code() {
        return value;
    }
}
