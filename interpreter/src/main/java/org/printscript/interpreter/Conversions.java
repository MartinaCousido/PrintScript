package org.printscript.interpreter;

import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

// lleva un valor al tipo con el que se declaró la variable
// se aplica SIEMPRE, en la declaración y en la asignación, y no solo cuando el
// inicializador es una lectura externa: para todo lo demás es un no-op, porque semantic
// ya garantizó que el tipo coincide. Así el Executor no tiene que preguntar de qué nodo
// vino el valor, que es justo el enredo por el que en el repo relevado el validador y
// el ejecutor terminan diciendo cosas distintas.
final class Conversions {

    private Conversions() {}

    static Result<Object> toDeclaredType(String declaredType, Object value, Range range) {
        if (!(value instanceof String text)) {
            return new Success<>(value);
        }
        return switch (declaredType) {
            case "string" -> new Success<>(text);
            case "number" -> toNumber(text, range);
            case "boolean" -> toBoolean(text, range);
            default -> new Success<>(value);
        };
    }

    private static Result<Object> toNumber(String text, Range range) {
        try {
            return new Success<>(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return new Failure<>("'" + text + "' no es un number", range);
        }
    }

    // estricto a propósito: solo true y false en minúscula, que es lo testeable
    private static Result<Object> toBoolean(String text, Range range) {
        if ("true".equals(text) || "false".equals(text)) {
            return new Success<>(Boolean.parseBoolean(text));
        }
        return new Failure<>("'" + text + "' no es un boolean", range);
    }
}
