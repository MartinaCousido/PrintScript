package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.runtime.OutputPrinter;

class ExecutionRunnerTest {

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

    @Test
    void executesMinimalProgramAndPrintsExpectedOutput() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "let x: number = 1 + 2;", "println(x);");

        assertEquals(List.of("3"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void executesProgramWithStringsAndDeclarationsAcrossLines() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let greeting: string = \"hola\";",
                        "let name: string = \"mundo\";",
                        "println(greeting + \" \" + name);");

        assertEquals(List.of("hola mundo"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void emptyProgramProducesNoOutputAndNoFailures() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer);

        assertEquals(List.of(), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void accumulatesFailuresWithoutStoppingExecution() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "println(\"antes\");",
                        "println(desconocida);",
                        "println(\"despues\");");

        assertEquals(List.of("antes", "despues"), printer.lines);
        assertEquals(1, failures.size());
        assertNotNull(failures.getFirst().message());
        assertNotNull(failures.getFirst().range());
    }

    @Test
    void outputBeforeTheErrorIsAlreadyEmitted() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "println(\"primera\");", "let x: number = y;");

        assertEquals(List.of("primera"), printer.lines);
        assertEquals(1, failures.size());
    }

    @Test
    void collectsEveryFailureOfTheProgram() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "println(a);", "println(b);", "println(\"fin\");");

        assertEquals(2, failures.size());
        assertEquals(List.of("fin"), printer.lines);
    }

    @Test
    void reportsParseErrorsAsFailures() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runProgram(printer, "let x: number = 1");

        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().message().contains(";"));
        assertEquals(List.of(), printer.lines);
    }
}
