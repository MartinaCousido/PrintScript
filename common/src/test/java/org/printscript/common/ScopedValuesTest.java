package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScopedValuesTest {

    private static final String NAME = "name";
    private static final String COUNT = "count";

    @Test
    void innerScopeShadowsOuterValueWithoutMutatingIt() {
        ScopedValues<String> outer =
                new ScopedValues<String>().declare(NAME, "outer").orElseThrow();
        ScopedValues<String> inner = outer.openScope().declare(NAME, "inner").orElseThrow();

        assertEquals(Optional.of("inner"), inner.read(NAME));
        assertEquals(Optional.of("outer"), inner.closeScope().read(NAME));
        assertEquals(Optional.of("outer"), outer.read(NAME));
    }

    @Test
    void declarationRejectsOnlyNamesInTheCurrentScope() {
        ScopedValues<Integer> values = new ScopedValues<Integer>().declare(COUNT, 1).orElseThrow();

        assertTrue(values.declare(COUNT, 2).isEmpty());
        assertEquals(
                Optional.of(2), values.openScope().declare(COUNT, 2).orElseThrow().read(COUNT));
    }

    @Test
    void replaceUpdatesNearestVisibleValue() {
        ScopedValues<Integer> outer = new ScopedValues<Integer>().declare(COUNT, 1).orElseThrow();
        ScopedValues<Integer> inner = outer.openScope().replace(COUNT, 2).orElseThrow();

        assertEquals(Optional.of(2), inner.read(COUNT));
        assertEquals(Optional.of(2), inner.closeScope().read(COUNT));
        assertEquals(Optional.of(1), outer.read(COUNT));
        assertTrue(inner.replace("missing", 3).isEmpty());
    }

    @Test
    void emptyReadsAndClosingOutermostScopeAreSafe() {
        ScopedValues<String> empty = new ScopedValues<>();

        assertTrue(empty.read("missing").isEmpty());
        assertSame(empty, empty.closeScope());
    }

    @Test
    void namesAndValuesCannotBeNull() {
        ScopedValues<String> values = new ScopedValues<>();

        assertThrows(NullPointerException.class, () -> values.declare(null, "value"));
        assertThrows(NullPointerException.class, () -> values.declare(NAME, null));
        assertThrows(NullPointerException.class, () -> values.read(null));
        assertThrows(NullPointerException.class, () -> values.replace(NAME, null));
    }

    @Test
    void deepScopesAndOuterAssignmentsPreserveHistoricalSnapshots() {
        final int depth = 10_000;
        ScopedValues<Integer> original =
                new ScopedValues<Integer>().declare(COUNT, 1).orElseThrow();
        ScopedValues<Integer> nested = original;
        for (int index = 0; index < depth; index++) {
            nested = nested.openScope();
        }
        ScopedValues<Integer> changed = nested.replace(COUNT, 2).orElseThrow();
        assertEquals(Optional.of(1), nested.read(COUNT));
        assertEquals(Optional.of(2), changed.read(COUNT));
        for (int index = 0; index < depth; index++) {
            changed = changed.closeScope();
        }
        assertEquals(Optional.of(2), changed.read(COUNT));
        assertEquals(Optional.of(1), original.read(COUNT));
    }
}
