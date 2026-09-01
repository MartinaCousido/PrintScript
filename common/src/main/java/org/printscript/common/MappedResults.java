package org.printscript.common;

import java.util.function.Function;

public final class MappedResults<S, T> implements PrintScriptIterator<Result<T>> {

    private final PrintScriptIterator<Result<S>> source;
    private final Function<S, T> mapping;

    public MappedResults(PrintScriptIterator<Result<S>> source, Function<S, T> mapping) {
        this.source = source;
        this.mapping = mapping;
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public Result<T> getNext() {
        Result<S> next = source.getNext();
        if (next instanceof Failure<S> failure) {
            return new Failure<>(failure.message(), failure.range());
        }
        return new Success<>(mapping.apply(((Success<S>) next).value()));
    }
}
