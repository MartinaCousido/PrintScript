package org.printscript.cli;

import java.io.PrintStream;
import org.printscript.runtime.OutputPrinter;

// el puerto por el que el programa en ejecución habla con el mundo de afuera;
// cli es el único módulo autorizado a escribir directamente en la salida estándar
// (docs/arquitectura.md, sección 5). El stream destino es inyectable para que la
// interfaz interactiva pueda dirigir la salida a su propio canal.
final class ConsoleOutputPrinter implements OutputPrinter {

    private final PrintStream target;

    ConsoleOutputPrinter() {
        this(System.out);
    }

    ConsoleOutputPrinter(PrintStream target) {
        this.target = target;
    }

    @Override
    public void print(String line) {
        target.println(line);
    }
}
