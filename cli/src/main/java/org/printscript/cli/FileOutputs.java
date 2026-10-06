package org.printscript.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes beside the destination and replaces it only after the operation succeeds. */
final class FileOutputs {
    @FunctionalInterface
    interface Operation {
        ExitCode write(Writer destination);
    }

    private FileOutputs() {}

    static ExitCode write(Path output, PrintStream err, Operation operation) {
        Path temporary = null;
        try {
            Path destination = output.toAbsolutePath().normalize();
            temporary = Files.createTempFile(destination.getParent(), ".printscript-", ".tmp");
            ExitCode result;
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                result = operation.write(writer);
            }
            if (result == ExitCode.SUCCESS) {
                replace(temporary, destination);
            }
            return result;
        } catch (IOException | UncheckedIOException exception) {
            err.println("No se pudo escribir '" + output + "': " + exception.getMessage());
            return ExitCode.USAGE_ERROR;
        } finally {
            removeTemporary(temporary, err);
        }
    }

    private static void replace(Path temporary, Path destination) throws IOException {
        try {
            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporary(Path temporary, PrintStream err) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException exception) {
            err.println(
                    "No se pudo eliminar el temporal '"
                            + temporary
                            + "': "
                            + exception.getMessage());
        }
    }
}
