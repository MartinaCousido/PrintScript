package org.printscript.semantic;

import java.util.Optional;
import java.util.Set;

// el tipo semántico, con el que se razona sobre qué programas tienen sentido
// no confundir con TypeName, que es la anotación tal como fue escrita en el fuente:
// uno es sintaxis y el otro es semántica, y por eso viven en módulos distintos
public enum Type {
    NUMBER("number"),
    STRING("string"),
    BOOLEAN("boolean");

    private final String written;

    Type(String written) {
        this.written = written;
    }

    // traduce la anotación escrita al tipo semántico
    // vacío si ese tipo no existe en la versión que se está procesando: "boolean" es un
    // tipo válido en 1.1 y una anotación desconocida en 1.0
    public static Optional<Type> fromWrittenName(String written, Set<Type> available) {
        for (Type type : available) {
            if (type.written.equals(written)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public String writtenName() {
        return written;
    }
}
