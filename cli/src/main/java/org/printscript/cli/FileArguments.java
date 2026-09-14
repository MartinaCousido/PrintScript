package org.printscript.cli;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

// abre y valida archivos
final class FileArguments {

    private FileArguments() {}

    static boolean requireExistingRegularFile(Path path, PrintStream err) {
        if (!Files.isRegularFile(path)) {
            err.println("El archivo '" + path + "' no existe o no es un archivo regular.");
            return false;
        }
        return true;
    }
}
