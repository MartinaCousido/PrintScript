package org.printscript.cli;

// los códigos de salida que puede devolver el proceso, ver docs/cli.md
public enum ExitCode {
    SUCCESS(0),
    SOURCE_ERRORS(1),
    USAGE_ERROR(2),
    UNSUPPORTED_OPERATION(3);

    private final int value;

    ExitCode(int value) {
        this.value = value;
    }

    public int code() {
        return value;
    }
}
