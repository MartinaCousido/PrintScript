package org.printscript.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

// evita repetir "si falló, propagalo" en cada etapa
public final class MappedResults<S, T> implements Iterator<Result<T>> {

    private final Iterator<Result<S>> source;
    private final Function<S, T> mapping;

    public MappedResults(Iterator<Result<S>> source, Function<S, T> mapping) {
        this.source = Objects.requireNonNull(source, "source");
        this.mapping = Objects.requireNonNull(mapping, "mapping");
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public Result<T> next() {
        return source.next().map(mapping);
    }
}
