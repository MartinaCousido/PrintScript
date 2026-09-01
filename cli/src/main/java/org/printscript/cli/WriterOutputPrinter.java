package org.printscript.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import org.printscript.runtime.OutputPrinter;

// OutputPrinter que escribe cada línea del programa a un Writer (un archivo, en la
// práctica). cli es el único módulo autorizado a tocar el filesystem
// (docs/arquitectura.md, sección 5).
final class WriterOutputPrinter implements OutputPrinter {

    private final Writer writer;

    WriterOutputPrinter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void print(String line) {
        try {
            writer.write(line);
            writer.write("\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
