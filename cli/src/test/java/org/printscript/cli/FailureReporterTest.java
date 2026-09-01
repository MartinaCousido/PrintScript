package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;

class FailureReporterTest {

    @Test
    void reportsMessageAndFullRange() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FailureReporter reporter =
                new FailureReporter(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        reporter.report(
                List.of(new Failure<>("boom", new Range(new Position(3, 4), new Position(3, 9)))));

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("boom"));
        assertTrue(output.contains("línea 3, columna 4"));
        assertTrue(output.contains("línea 3, columna 9"));
    }

    @Test
    void reportsNothingWhenThereAreNoFailures() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FailureReporter reporter =
                new FailureReporter(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        reporter.report(List.of());

        assertTrue(buffer.toString(StandardCharsets.UTF_8).isEmpty());
    }
}
