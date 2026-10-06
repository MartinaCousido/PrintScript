package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class PersistentNamesTest {
    private static final int STRESS_SIZE = 20_000;
    private static final int RANDOM_SEED = 42;

    @Test
    void orderedAndReverseOrderedInsertionsRetainAllValuesAndOldSnapshots() {
        checkOrdered(false);
        checkOrdered(true);
    }

    private static void checkOrdered(boolean reverse) {
        PersistentNames<Integer> values = new PersistentNames<>();
        PersistentNames<Integer> empty = values;
        for (int index = 0; index < STRESS_SIZE; index++) {
            int number = reverse ? STRESS_SIZE - index - 1 : index;
            values = values.put(Integer.toString(number), number);
        }
        for (int index = 0; index < STRESS_SIZE; index++) {
            assertEquals(Optional.of(index), values.read(Integer.toString(index)));
        }
        assertTrue(empty.read("0").isEmpty());
        PersistentNames<Integer> changed = values.put("0", -1);
        assertEquals(Optional.of(0), values.read("0"));
        assertEquals(Optional.of(-1), changed.read("0"));
        assertTrue(values.read("missing").isEmpty());
    }

    @Test
    void randomizedWritesMatchAnIndependentSortedMapIncludingHashCollisions() {
        Random random = new Random(RANDOM_SEED);
        Map<String, Integer> expected = new TreeMap<>();
        PersistentNames<Integer> actual = new PersistentNames<>();
        for (int index = 0; index < STRESS_SIZE; index++) {
            String name = Integer.toString(random.nextInt(STRESS_SIZE));
            expected.put(name, index);
            actual = actual.put(name, index);
        }
        actual = actual.put("Aa", 1).put("BB", 2);
        expected.put("Aa", 1);
        expected.put("BB", 2);
        for (var entry : expected.entrySet()) {
            assertEquals(Optional.of(entry.getValue()), actual.read(entry.getKey()));
        }
    }
}
