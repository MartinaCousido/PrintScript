package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class MappedResultsTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));

    private static final class ListResults implements Iterator<Result<String>> {

        private final List<Result<String>> items;
        private int index;

        private ListResults(List<Result<String>> items) {
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            return index < items.size();
        }

        @Override
        public Result<String> next() {
            Result<String> item = items.get(index);
            index += 1;
            return item;
        }

        private int consumed() {
            return index;
        }
    }

    private static MappedResults<String, Integer> lengthsOf(List<Result<String>> items) {
        return new MappedResults<>(new ListResults(items), String::length);
    }

    @Test
    void aSuccessIsMapped() {
        MappedResults<String, Integer> lengths = lengthsOf(List.of(new Success<>("hola")));

        assertEquals(4, ((Success<Integer>) lengths.next()).value());
    }

    @Test
    void aFailurePassesThroughWithItsMessageAndRange() {
        MappedResults<String, Integer> lengths =
                lengthsOf(List.of(new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "roto", ANY)));

        Failure<Integer> failure = assertInstanceOf(Failure.class, lengths.next());
        assertEquals("roto", failure.message());
        assertEquals(ANY, failure.range());
    }

    @Test
    void thereIsNothingToMapWhenTheSourceIsEmpty() {
        assertFalse(lengthsOf(List.of()).hasNext());
    }

    @Test
    void theSourceIsOnlyAdvancedOnDemand() {
        ListResults source = new ListResults(List.of(new Success<>("hola"), new Success<>("chau")));
        MappedResults<String, Integer> lengths = new MappedResults<>(source, String::length);

        lengths.next();

        assertEquals(1, source.consumed());
        assertTrue(lengths.hasNext());
    }
}
