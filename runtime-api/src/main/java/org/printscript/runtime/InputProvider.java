package org.printscript.runtime;

import java.util.Optional;

// es por donde el programa en ejecución le pide una línea al usuario
// vive acá y no en el intérprete para que el cli pueda implementarlo sin depender del
// intérprete entero
// devuelve Optional porque quedarse sin entrada es una situación normal, no una
// excepción: el intérprete la reporta como cualquier otro error, con su rango
@FunctionalInterface
public interface InputProvider {

    Optional<String> readLine();
}
