package org.printscript.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

// abre y valida archivos
final class FileArguments {

    private FileArguments() {}

    static boolean requireSeparateOutput(Path source, Path output, PrintStream err) {
        if (output == null) {
            return true;
        }
        try {
            boolean samePath =
                    source.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize());
            boolean sameFile = Files.exists(output) && Files.isSameFile(source, output);
            if (samePath || sameFile) {
                err.println("La salida debe ser distinta del archivo fuente: " + output);
                return false;
            }
            return true;
        } catch (IOException e) {
            err.println("No se pudo verificar el archivo de salida: " + e.getMessage());
            return false;
        }
    }

    static boolean requireExistingRegularFile(Path path, PrintStream err) {
        if (!Files.isRegularFile(path)) {
            err.println("El archivo '" + path + "' no existe o no es un archivo regular.");
            return false;
        }
        return true;
    }
}
