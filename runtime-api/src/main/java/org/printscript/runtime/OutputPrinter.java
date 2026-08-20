package org.printscript.runtime;

// es por donde el programa en ejecución le habla al mundo de afuera
@FunctionalInterface
public interface OutputPrinter {

    void print(String line);
}
