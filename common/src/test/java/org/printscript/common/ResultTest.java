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
        Result<Integer> result = new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "boom", range);

        assertEquals("error:boom", describe(result));
    }

    @Test
    void failureExposesMessageAndRangeAsData() {
        Range range = new Range(new Position(2, 3), new Position(2, 5));
        Failure<Integer> failure = new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "boom", range);

        assertEquals("boom", failure.message());
        assertEquals(range, failure.range());
    }

    @Test
    void mapTransformsTheValueOfASuccess() {
        Result<Integer> result = new Success<Integer>(21).map(value -> value * 2);

        assertEquals("value:42", describe(result));
    }

    @Test
    void mapLeavesAFailureUntouchedAndDoesNotRunTheMapping() {
        Range range = new Range(new Position(1, 1), new Position(1, 3));
        Result<Integer> original = new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "roto", range);

        Result<Integer> result = original.map(value -> value * 2);

        assertEquals("error:roto", describe(result));
        assertEquals(range, ((Failure<Integer>) result).range());
    }

    @Test
    void chainingAFailureKeepsItsCode() {
        Range range = new Range(new Position(1, 1), new Position(1, 3));
        Result<Integer> original = new Failure<>(ErrorCode.MISSING_SEMICOLON, "falta ';'", range);

        Failure<?> mapped = (Failure<?>) original.map(value -> value * 2);
        Failure<?> chained = (Failure<?>) original.andThen(value -> new Success<>(value + 1));

        assertEquals(ErrorCode.MISSING_SEMICOLON, mapped.code());
        assertEquals(ErrorCode.MISSING_SEMICOLON, chained.code());
    }

    @Test
    void andThenChainsTheNextStepWhenEverythingSucceeds() {
        Result<Integer> result =
                new Success<Integer>(20).andThen(value -> new Success<>(value + 22));

        assertEquals("value:42", describe(result));
    }

    @Test
    void andThenPropagatesTheFailureOfTheNextStep() {
        Range range = new Range(new Position(4, 2), new Position(4, 6));

        Result<Integer> result =
                new Success<Integer>(20)
                        .andThen(
                                value ->
                                        new Failure<>(
                                                ErrorCode.UNRECOGNIZED_STATEMENT,
                                                "segundo",
                                                range));

        assertEquals("error:segundo", describe(result));
        assertEquals(range, ((Failure<Integer>) result).range());
    }

    @Test
    void andThenShortCircuitsOnFailureWithoutRunningTheNextStep() {
        Range range = new Range(new Position(7, 1), new Position(7, 4));
        Result<Integer> original =
                new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "primero", range);

        Result<Integer> result =
                original.andThen(
                        value -> {
                            throw new AssertionError("no se debe ejecutar el paso siguiente");
                        });

        assertEquals("error:primero", describe(result));
        assertEquals(range, ((Failure<Integer>) result).range());
    }

    @Test
    void combinatorsChainSeveralStepsAndStopAtTheFirstFailure() {
        Range range = new Range(new Position(3, 3), new Position(3, 8));

        Result<Integer> result =
                new Success<Integer>(2)
                        .map(value -> value * 10)
                        .andThen(value -> new Success<>(value + 1))
                        .andThen(
                                value ->
                                        new Failure<Integer>(
                                                ErrorCode.UNRECOGNIZED_STATEMENT, "cortado", range))
                        .map(value -> value * 100);

        assertEquals("error:cortado", describe(result));
    }
}
