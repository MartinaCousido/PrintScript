package org.printscript.runtime;

// los tres puertos por los que el programa en ejecución habla con el mundo de afuera
// van juntos porque siempre viajan juntos: quien arma el intérprete los provee de a
// tres, y así agregar un puerto nuevo no cambia la firma de todo el camino
public record RuntimePorts(OutputPrinter printer, InputProvider input, EnvProvider env) {}
