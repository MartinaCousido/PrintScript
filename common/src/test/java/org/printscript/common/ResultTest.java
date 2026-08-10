package org.printscript.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResultTest {

    private static String describe(Result<Integer> result) {
        return switch (result) {
            case Success<Integer> success -> "value:" + success.value();
            case Failure<Integer> failure -> "error:" + failure.message();
        };
    }

    @Test
    void successCarriesItsValueAndIsConsumedByPatternMatching() {
        Result<Integer> result = new Success<>(42);

        assertEquals("value:42", describe(result));
    }

    @Test
    void failureCarriesMessageAndRangeAndIsConsumedByPatternMatching() {
        Range range = new Range(new Position(2, 3), new Position(2, 5));
        Result<Integer> result = new Failure<>("boom", range);

        assertEquals("error:boom", describe(result));
    }

    @Test
    void failureExposesMessageAndRangeAsData() {
        Range range = new Range(new Position(2, 3), new Position(2, 5));
        Failure<Integer> failure = new Failure<>("boom", range);

        assertEquals("boom", failure.message());
        assertEquals(range, failure.range());
    }
}
