package org.printscript.runtime;

// es por donde el programa en ejecución le habla al mundo de afuera
public interface OutputPrinter {

    // una línea de salida del programa, con su salto al final
    void println(String line);

    // texto suelto, sin salto de línea
    void print(String text);
}
