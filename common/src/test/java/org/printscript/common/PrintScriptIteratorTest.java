package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class PrintScriptIteratorTest {

    private static final class ListIterator<T> implements PrintScriptIterator<T> {

        private final List<T> elements;
        private int index;

        private ListIterator(List<T> elements) {
            this.elements = elements;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < elements.size();
        }

        @Override
        public T getNext() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T element = elements.get(index);
            index = index + 1;
            return element;
        }
    }

    @Test
    void getNextReturnsElementsInOrder() {
        PrintScriptIterator<String> iterator = new ListIterator<>(List.of("a", "b"));

        assertEquals("a", iterator.getNext());
        assertEquals("b", iterator.getNext());
    }

    @Test
    void getNextWithoutAvailableElementsThrowsNoSuchElementException() {
        PrintScriptIterator<String> iterator = new ListIterator<>(List.of());

        assertThrows(NoSuchElementException.class, iterator::getNext);
    }

    @Test
    void hasNextIsIdempotentAndDoesNotConsume() {
        PrintScriptIterator<String> iterator = new ListIterator<>(List.of("only"));

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertEquals("only", iterator.getNext());

        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }
}
