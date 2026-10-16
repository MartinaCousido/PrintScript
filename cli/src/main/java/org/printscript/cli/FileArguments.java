package org.printscript.cli;

import java.io.IOException;
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

    static boolean requireDistinctOutput(Path source, Path output, PrintStream err) {
        if (output == null) {
            return true;
        }
        try {
            boolean samePath =
                    source.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize());
            boolean sameFile = Files.exists(output) && Files.isSameFile(source, output);
            if (samePath || sameFile) {
                err.println("El archivo de salida debe ser distinto del fuente: " + output);
                return false;
            }
            return true;
        } catch (IOException exception) {
            err.println(
                    "No se pudo comprobar la salida '" + output + "': " + exception.getMessage());
            return false;
        }
    }
}
