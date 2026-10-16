package org.printscript.common;

import java.util.function.Function;

// la respuesta de cualquier operación: salió bien o falló
public sealed interface Result<T> permits Success, Failure {

    // transforma el valor
    default <R> Result<R> map(Function<? super T, ? extends R> mapping) {
        return switch (this) {
            case Success<T> success -> new Success<>(mapping.apply(success.value()));
            case Failure<T> failure ->
                    new Failure<>(failure.code(), failure.message(), failure.range());
        };
    }

    default <R> Result<R> andThen(Function<? super T, ? extends Result<R>> next) {
        return switch (this) {
            case Success<T> success -> next.apply(success.value());
            case Failure<T> failure ->
                    new Failure<>(failure.code(), failure.message(), failure.range());
        };
    }
}
