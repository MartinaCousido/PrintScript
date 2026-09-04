package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.runtime.OutputPrinter;

class ExecutionErrorsEndToEndTest {

    private static final String ANTES = "antes";
    private static final String PRINTLN_ANTES = "println(\"antes\");";

    private static final class InMemoryPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void println(String line) {
            lines.add(line);
        }

        @Override
        public void print(String text) {
            lines.add(text);
        }
    }

    private static List<Failure<?>> runProgram(InMemoryPrinter printer, String... lines) {
        return new ExecutionRunner().run(List.of(lines).iterator(), printer);
    }

    private static void assertRange(
            Range range, int startLine, int startColumn, int endLine, int endColumn) {
        assertEquals(startLine, range.start().line());
        assertEquals(startColumn, range.start().column());
        assertEquals(endLine, range.end().line());
        assertEquals(endColumn, range.end().column());
    }

    @Test
    void missingSemicolonReportsTheIntrudingTokenAndLaterStatementsStillRun() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        PRINTLN_ANTES,
                        "let x: number = 1",
                        "println(\"tragada\");",
                        "println(\"fin\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Expected ';' at the end of the declaration", failure.message());
        assertRange(failure.range(), 3, 1, 3, 7);
        assertEquals(List.of(ANTES, "fin"), printer.lines);
    }

    @Test
    void missingColonInDeclarationReportsTheTokenFoundInItsPlaceAndTheNextStatementRuns() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "let x number = 1;", "println(\"despues\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Expected ':' after the variable name", failure.message());
        assertRange(failure.range(), 1, 7, 1, 12);
        assertEquals(List.of("despues"), printer.lines);
    }

    @Test
    void unexpectedTokenInOperandPositionReportsItsExactRangeAndTheNextStatementRuns() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "let a: number = *;", "println(\"sigue\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Unexpected token '*' in operand position", failure.message());
        assertRange(failure.range(), 1, 17, 1, 17);
        assertEquals(List.of("sigue"), printer.lines);
    }

    @Test
    void undeclaredVariableInPrintlnPointsToTheOffendingIdentifier() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "println(desconocida);");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Variable no declarada: desconocida", failure.message());
        assertRange(failure.range(), 1, 9, 1, 19);
        assertEquals(List.of(), printer.lines);
    }

    @Test
    void subtractingTwoStringsPointsToTheOffendingExpression() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let a: string = \"x\";",
                        "let b: string = \"y\";",
                        "println(a - b);");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("No se puede aplicar MINUS entre string y string", failure.message());
        assertRange(failure.range(), 3, 9, 3, 13);
        assertEquals(List.of(), printer.lines);
    }

    @Test
    void unknownCharacterInTheMiddleOfTheSourceDoesNotStopTheRestOfTheProgram() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, PRINTLN_ANTES, "@;", "println(\"despues\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Unrecognized statement", failure.message());
        assertRange(failure.range(), 2, 1, 2, 1);
        assertEquals(List.of(ANTES, "despues"), printer.lines);
    }

    @Test
    void printlnWithSyntaxErrorReachesTheConsumerWithoutPrintingAnything() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, PRINTLN_ANTES, "println(\"rota\";", "println(\"despues\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Expected ')' after the println argument", failure.message());
        assertRange(failure.range(), 2, 15, 2, 15);
        assertEquals(List.of(ANTES, "despues"), printer.lines);
    }

    @Test
    void missingSemicolonAtTheEndOfTheStreamReportsTheLastToken() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, PRINTLN_ANTES, "let x: number = 42");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Expected ';' before the end of the stream", failure.message());
        assertRange(failure.range(), 2, 17, 2, 18);
        assertEquals(List.of(ANTES), printer.lines);
    }
}
