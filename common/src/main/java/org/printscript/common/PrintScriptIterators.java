package org.printscript.common;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// adapta el iterador perezoso propio del pipeline a las interfaces estándar de
// Java, para consumirlo con for-each o con Stream en vez de un while a mano.
// No rompe la pereza: sigue pidiendo un elemento a la vez.
public final class PrintScriptIterators {

    private PrintScriptIterators() {}

    public static <T> Iterator<T> iterator(PrintScriptIterator<T> source) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public T next() {
                return source.getNext();
            }
        };
    }

    public static <T> Iterable<T> iterable(PrintScriptIterator<T> source) {
        return () -> iterator(source);
    }

    public static <T> Stream<T> stream(PrintScriptIterator<T> source) {
        Spliterator<T> spliterator =
                Spliterators.spliteratorUnknownSize(iterator(source), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }
}
