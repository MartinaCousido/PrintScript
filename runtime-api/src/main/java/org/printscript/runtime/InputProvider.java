package org.printscript.runtime;

// es por donde el programa en ejecución le pide una línea al usuario
// vive acá y no en el intérprete para que el cli pueda implementarlo sin depender del
// intérprete entero
@FunctionalInterface
public interface InputProvider {

    String readLine();
}
