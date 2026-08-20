package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;

class ValidationRunnerTest {

    private static final String VERSION = "1.0";

    private static List<Failure<?>> validateProgram(String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), VERSION);
    }

    // originalOut es un alias de System.out para restaurarlo en el finally, no un
    // recurso propio que haya que cerrar.
    @Test
    @SuppressWarnings("PMD.CloseResource")
    void validProgramWithPrintlnProducesNoFailuresAndNoOutput() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            List<Failure<?>> failures = validateProgram("let x: number = 1 + 2;", "println(x);");

            assertEquals(List.of(), failures);
            assertEquals("", captured.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void emptyProgramProducesNoFailures() {
        List<Failure<?>> failures = validateProgram();

        assertEquals(List.of(), failures);
    }

    @Test
    void reportsMissingSemicolonWithMessageAndRange() {
        List<Failure<?>> failures = validateProgram("let x: number = 1");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertTrue(failure.message().contains(";"));
        assertEquals(new Range(new Position(1, 17), new Position(1, 17)), failure.range());
    }

    @Test
    void reportsUnrecognizedStatementWithFullRangeOnItsLine() {
        List<Failure<?>> failures = validateProgram("let x: number = 1;", "42;");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Unrecognized statement", failure.message());
        assertEquals(new Position(2, 1), failure.range().start());
        assertEquals(new Position(2, 2), failure.range().end());
    }

    @Test
    void collectsEverySyntaxErrorOfTheProgram() {
        List<Failure<?>> failures = validateProgram("42;", "println(\"ok\");", "7;");

        assertEquals(2, failures.size());
    }

    @Test
    void unknownVersionFailsWithClearMessage() {
        ValidationRunner runner = new ValidationRunner();
        List<String> lines = List.of("println(1);");

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> runner.validate(lines.iterator(), "9.9"));

        assertTrue(error.getMessage().contains("9.9"));
    }
}
