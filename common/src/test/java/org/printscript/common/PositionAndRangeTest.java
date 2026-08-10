package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
