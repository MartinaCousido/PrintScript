package org.printscript.runtime;

// los tres puertos por los que el programa en ejecución habla con el mundo de afuera
public record RuntimePorts(OutputPrinter printer, InputProvider input, EnvProvider env) {}
