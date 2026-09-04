package org.printscript.semantic;

import java.util.Optional;

// el tipo semántico, con el que se razona sobre qué programas tienen sentido
// no confundir con TypeName, que es la anotación tal como fue escrita en el fuente:
// uno es sintaxis y el otro es semántica, y por eso viven en módulos distintos
public enum Type {
    NUMBER("number"),
    STRING("string");

    private final String written;

    Type(String written) {
        this.written = written;
    }

    // traduce la anotación escrita al tipo semántico; vacío si ese tipo no existe
    public static Optional<Type> fromWrittenName(String written) {
        for (Type type : values()) {
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
