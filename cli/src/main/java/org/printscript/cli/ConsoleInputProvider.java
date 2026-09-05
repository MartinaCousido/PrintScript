package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.printscript.runtime.InputProvider;

// lee una línea de la entrada estándar para readInput
// recibe el BufferedReader en vez de abrir uno propio a propósito: la interfaz
// interactiva ya tiene su lector sobre System.in para los comandos, y dos lectores
// sobre el mismo stream se comen las líneas del otro de forma intermitente
final class ConsoleInputProvider implements InputProvider {

    private final BufferedReader reader;

    ConsoleInputProvider(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public Optional<String> readLine() {
        try {
            return Optional.ofNullable(reader.readLine());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
