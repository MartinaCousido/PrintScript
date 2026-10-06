package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

class StreamingDiagnosticsTest {
    private static final int ERROR_COUNT = 10_000;
    private static final String INVALID = "let broken number = 1;";

    @Test
    void validationReportsEachFailureBeforeRequestingTheNextLine() {
        AtomicInteger reported = new AtomicInteger();
        assertFalse(
                new ValidationRunner()
                        .validate(
                                errorLines(reported),
                                Version.V11,
                                failure -> reported.incrementAndGet()));
        assertEquals(ERROR_COUNT, reported.get());
        assertTrue(
                new ValidationRunner()
                        .validate(
                                List.of("println(1);").iterator(),
                                Version.V10,
                                failure -> {
                                    throw new AssertionError(failure.message());
                                }));
    }

    @Test
    void executionAndAnalysisAlsoStreamFailuresWithoutAReportList() {
        AtomicInteger reported = new AtomicInteger();
        OutputPrinter printer =
                new OutputPrinter() {
                    @Override
                    public void println(String line) {}

                    @Override
                    public void print(String text) {}
                };
        RuntimePorts ports = new RuntimePorts(printer, Optional::empty, name -> Optional.empty());
        assertFalse(
                new ExecutionRunner()
                        .run(
                                errorLines(reported),
                                ports,
                                Version.V11,
                                failure -> reported.incrementAndGet()));
        assertEquals(ERROR_COUNT, reported.get());
        reported.set(0);
        assertFalse(
                new AnalyzingRunner()
                        .analyze(
                                errorLines(reported),
                                new LinterConfiguration(Optional.empty(), false, false),
                                Version.V11,
                                violation -> {
                                    throw new AssertionError(violation.message());
                                },
                                failure -> reported.incrementAndGet()));
        assertEquals(ERROR_COUNT, reported.get());
    }

    @Test
    void formattingStreamsLexicalFailuresAndFlushesTheBorrowedWriter() {
        AtomicInteger reported = new AtomicInteger();
        StringWriter destination = new StringWriter();
        assertFalse(
                new FormattingRunner()
                        .format(
                                List.of("@").iterator(),
                                FormatterConfiguration.none(),
                                destination,
                                Version.V10,
                                failure -> reported.incrementAndGet()));
        assertEquals(1, reported.get());
    }

    private static Iterator<String> errorLines(AtomicInteger reported) {
        return new Iterator<>() {
            private int consumed;

            @Override
            public boolean hasNext() {
                return consumed < ERROR_COUNT;
            }

            @Override
            public String next() {
                assertEquals(consumed, reported.get());
                consumed++;
                return INVALID;
            }
        };
    }
}
