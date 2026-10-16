package org.printscript.runtime;

import java.util.Objects;

// los tres puertos por los que el programa en ejecución habla con el mundo de afuera
public record RuntimePorts(OutputPrinter printer, InputProvider input, EnvProvider env) {

    public RuntimePorts {
        Objects.requireNonNull(printer, "printer");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(env, "env");
    }
}
