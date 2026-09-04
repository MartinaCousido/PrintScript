package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.runtime.OutputPrinter;

class AssignmentAndDeclarationEndToEndTest {

    private static final String PRINTLN_X = "println(x);";

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
    void declaringAssigningAndPrintingShowsTheNewValue() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "let x: number = 1;", "x = 42;", PRINTLN_X);

        assertEquals(List.of("42"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void variableKeepsOperatingAsItsDeclaredTypeAfterAssignment() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "let x: number = 10;", "x = 7;", "println(x - 2);");

        assertEquals(List.of("5"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void assigningToANeverDeclaredVariableFailsWithTheStatementRangeAndTheName() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "y = 1;", "println(\"sigue\");");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Variable no declarada: y", failure.message());
        assertRange(failure.range(), 1, 1, 1, 6);
        assertEquals(List.of("sigue"), printer.lines);
    }

    @Test
    void declarationWithoutInitializerThenAssignmentAndPrintlnRunsEndToEnd() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "let x: number;", "x = 5;", PRINTLN_X);

        assertEquals(List.of("5"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void redeclaringAVariableFailsWithTheRangeOfTheSecondDeclaration() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "let x: number = 1;", "let x: number = 2;", PRINTLN_X);

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Variable ya declarada: x", failure.message());
        assertRange(failure.range(), 2, 1, 2, 18);
        assertEquals(List.of("1"), printer.lines);
    }

    @Test
    void printingAnUninitializedVariableCurrentlyPrintsNull() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "let x: number;", PRINTLN_X);

        assertEquals(List.of("null"), printer.lines);
        assertEquals(List.of(), failures);
    }
}
