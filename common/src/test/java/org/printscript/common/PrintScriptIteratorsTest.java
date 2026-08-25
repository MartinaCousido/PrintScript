package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PrintScriptIteratorsTest {

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
    void iteratorExposesElementsInOrder() {
        Iterator<String> iterator =
                PrintScriptIterators.iterator(new ListIterator<>(List.of("a", "b")));

        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertEquals("b", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iterableCanBeUsedInAForEach() {
        Iterable<String> iterable =
                PrintScriptIterators.iterable(new ListIterator<>(List.of("a", "b", "c")));

        List<String> collected = new ArrayList<>();
        for (String element : iterable) {
            collected.add(element);
        }

        assertEquals(List.of("a", "b", "c"), collected);
    }

    @Test
    void streamCollectsElementsInOrder() {
        List<String> collected =
                PrintScriptIterators.stream(new ListIterator<>(List.of("a", "b", "c")))
                        .collect(Collectors.toList());

        assertEquals(List.of("a", "b", "c"), collected);
    }
}
