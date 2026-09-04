package org.printscript.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutputPrinterTest {

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

    @Test
    void thePrinterReceivesEachLineOfTheProgramInOrder() {
        InMemoryPrinter printer = new InMemoryPrinter();

        printer.print("Joe Doe");
        printer.print("Result: 3");

        assertEquals(List.of("Joe Doe", "Result: 3"), printer.lines);
    }
}
