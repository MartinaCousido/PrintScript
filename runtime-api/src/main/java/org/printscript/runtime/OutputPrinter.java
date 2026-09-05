package org.printscript.runtime;

// es por donde el programa en ejecución le habla al mundo de afuera
// los dos métodos existen porque no toda la salida es una línea: el prompt de readInput
// tiene que quedar en la misma línea que lo que el usuario escribe. Un solo método
// haría que el prompt siempre saltara de línea, y eso no se puede arreglar después
// desde el intérprete.
public interface OutputPrinter {

    // una línea de salida del programa, con su salto al final
    void println(String line);

    // texto suelto, sin salto de línea
    void print(String text);
}
