package org.printscript.runtime;

import java.util.Optional;

// es por donde el programa en ejecución lee una variable de entorno
@FunctionalInterface
public interface EnvProvider {

    Optional<String> read(String name);
}
