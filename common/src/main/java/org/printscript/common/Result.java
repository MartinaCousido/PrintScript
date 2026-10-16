package org.printscript.common;

import java.util.function.Function;

// la respuesta de cualquier operación: salió bien o falló
public sealed interface Result<T> permits Success, Failure {

    // Encadenar sin estos dos obliga a cada llamador a desarmar el Result a mano:
    // pedir, preguntar si es Failure, devolver, castear a Success y sacar el valor.
    // Son seis líneas por paso, y el parser encadena hasta cinco pasos seguidos.
    // Acá el corte ante el primer error se escribe una sola vez.

    // el paso siguiente no puede fallar: solo transforma el valor
    default <R> Result<R> map(Function<? super T, ? extends R> mapping) {
        return switch (this) {
            case Success<T> success -> new Success<>(mapping.apply(success.value()));
            case Failure<T> failure ->
                    new Failure<>(failure.code(), failure.message(), failure.range());
        };
    }

    // el paso siguiente también devuelve un Result, así que puede fallar y cortar la cadena
    default <R> Result<R> andThen(Function<? super T, ? extends Result<R>> next) {
        return switch (this) {
            case Success<T> success -> next.apply(success.value());
            case Failure<T> failure ->
                    new Failure<>(failure.code(), failure.message(), failure.range());
        };
    }
}
