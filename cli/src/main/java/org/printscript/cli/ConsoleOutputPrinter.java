package org.printscript.cli;

import java.io.PrintStream;
import org.printscript.runtime.OutputPrinter;

// el puerto por el que el programa en ejecución habla con el mundo de afuera.
// escribe en System.out
final class ConsoleOutputPrinter implements OutputPrinter {

    private final PrintStream target;

    ConsoleOutputPrinter() {
        this(System.out);
    }

    ConsoleOutputPrinter(PrintStream target) {
        this.target = target;
    }

    @Override
    public void println(String line) {
        target.println(line);
    }

    @Override
    public void print(String text) {
        target.print(text);
        target.flush();
    }
}
