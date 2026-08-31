package org.printscript.cli;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

// abrir y validar archivos es responsabilidad exclusiva de cli, ver
// docs/arquitectura.md seccion 5
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
