package org.printscript.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import org.printscript.runtime.OutputPrinter;

// OutputPrinter que escribe cada línea del programa a un archivo
final class WriterOutputPrinter implements OutputPrinter {

    private final Writer writer;

    WriterOutputPrinter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void println(String line) {
        print(line + "\n");
    }

    @Override
    public void print(String text) {
        try {
            writer.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
