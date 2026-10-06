package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PositionAndRangeTest {

    @Test
    void linesAndColumnsStartAtOne() {
        Position position = new Position(1, 1);

        assertEquals(1, position.line());
        assertEquals(1, position.column());
    }

    @Test
    void rangeEndPointsToTheLastCharacterInclusive() {
        Position start = new Position(1, 5);
        Position end = new Position(1, 7);

        Range range = new Range(start, end);

        assertEquals(start, range.start());
        assertEquals(end, range.end());

        int lexemeLength = range.end().column() - range.start().column() + 1;
        assertEquals(3, lexemeLength);
    }

    @Test
    void positionsAndRangesHaveValueEquality() {
        Range one = new Range(new Position(1, 5), new Position(1, 7));
        Range other = new Range(new Position(1, 5), new Position(1, 7));

        assertEquals(one, other);
    }

    @Test
    void positionsRejectZeroBasedCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Position(1, 0));
    }

    @Test
    void rangesRejectReversedEndpoints() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Range(new Position(2, 1), new Position(1, 9)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Range(new Position(1, 5), new Position(1, 4)));
    }

    @Test
    void rangesRejectMissingEndpoints() {
        Position position = new Position(1, 1);
        assertThrows(NullPointerException.class, () -> new Range(null, position));
        assertThrows(NullPointerException.class, () -> new Range(position, null));
    }
}
