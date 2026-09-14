package org.printscript.runtime;

import java.util.Optional;

// es por donde el programa en ejecución le pide una línea al usuario
@FunctionalInterface
public interface InputProvider {

    Optional<String> readLine();
}
