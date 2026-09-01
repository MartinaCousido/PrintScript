package org.printscript.cli;

import java.util.List;
import org.printscript.runtime.OutputPrinter;

// reparte cada línea del programa a varios OutputPrinter a la vez; lo usa run-and-save
// para mostrar la salida por consola y guardarla en un archivo en la misma pasada
final class TeeOutputPrinter implements OutputPrinter {

    private final List<OutputPrinter> targets;

    TeeOutputPrinter(OutputPrinter... targets) {
        this.targets = List.of(targets);
    }

    @Override
    public void print(String line) {
        for (OutputPrinter target : targets) {
            target.print(line);
        }
    }
}
