package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.runtime.OutputPrinter;

class ConsignaExamplesEndToEndTest {

    private static final String VERSION = "1.0";

    private static final class InMemoryPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String line) {
            lines.add(line);
        }
    }

    private static List<Failure<?>> runProgram(InMemoryPrinter printer, String... lines) {
        return new ExecutionRunner().run(List.of(lines).iterator(), VERSION, printer);
    }

    @Test
    void firstExampleDeclaresStringsAndConcatenatesThem() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let name: string = \"Joe\";",
                        "let lastName: string = \"Doe\";",
                        "println(name + \" \" + lastName);");

        assertEquals(List.of("Joe Doe"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void secondExampleDividesAndPrintsAnIntegerResultWithoutADecimalPoint() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let a: number = 12;",
                        "let b: number = 4;",
                        "let c: number = a / b;",
                        "println(\"Result: \" + c);");

        assertEquals(List.of("Result: 3"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void thirdExampleUsesAssignmentInsteadOfInitializerAndPrintsTheSameResult() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let a: number = 12;",
                        "let b: number = 4;",
                        "a = a / b;",
                        "println(\"Result: \" + a);");

        assertEquals(List.of("Result: 3"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void aRealDecimalKeepsItsDecimalsInsteadOfBeingTruncated() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(printer, "let a: number = 7;", "let b: number = 2;", "println(a / b);");

        assertEquals(List.of("3.5"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void ownProgramCombinesDeclarationsArithmeticAndStringNumberConcatenation() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures =
                runProgram(
                        printer,
                        "let base: number = 10;",
                        "let increment: number = 5;",
                        "let total: number = base + increment;",
                        "let label: string = \"Total\";",
                        "println(label + \": \" + total);");

        assertEquals(List.of("Total: 15"), printer.lines);
        assertEquals(List.of(), failures);
    }
}
