package org.printscript.runtime;

import java.util.Optional;

// es por donde el programa en ejecución lee una variable de entorno
// que sea un puerto y no una llamada directa a System.getenv es lo que mantiene el
// invariante de que solo el cli toca el mundo exterior, y lo que hace que readEnv se
// pueda testear sin ensuciar el entorno de la máquina
@FunctionalInterface
public interface EnvProvider {

    Optional<String> read(String name);
}
