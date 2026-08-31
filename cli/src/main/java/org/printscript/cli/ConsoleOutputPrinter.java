package org.printscript.cli;

import org.printscript.runtime.OutputPrinter;

// el puerto por el que el programa en ejecución habla con el mundo de afuera;
// cli es el único módulo autorizado a escribir directamente en System.out
// (docs/arquitectura.md, sección 5)
final class ConsoleOutputPrinter implements OutputPrinter {

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void print(String line) {
        System.out.println(line);
    }
}
