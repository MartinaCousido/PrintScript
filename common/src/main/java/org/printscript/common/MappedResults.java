package org.printscript.common;

import java.util.Iterator;
import java.util.function.Function;

// evita repetir "si falló, propagalo" en cada etapa
public final class MappedResults<S, T> implements Iterator<Result<T>> {

    private final Iterator<Result<S>> source;
    private final Function<S, T> mapping;

    public MappedResults(Iterator<Result<S>> source, Function<S, T> mapping) {
        this.source = source;
        this.mapping = mapping;
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public Result<T> next() {
        Result<S> next = source.next();
        if (next instanceof Failure<S> failure) {
            return new Failure<>(failure.message(), failure.range());
        }
        return new Success<>(mapping.apply(((Success<S>) next).value()));
    }
}
