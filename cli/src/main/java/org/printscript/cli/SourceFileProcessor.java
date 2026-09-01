package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;

// abre el archivo fuente, arma el iterator de líneas con progreso, delega en la
// operación concreta y reporta las fallas. Es el único camino por el que cli lee un
// .ps del filesystem: lo comparten los subcomandos (AbstractSourceFileCommand) y la
// interfaz interactiva (InteractiveShell), así ambos se comportan idéntico.
final class SourceFileProcessor {

    // la operación concreta (execution/validation) sobre el stream de líneas
    @FunctionalInterface
    interface Operation {
        List<Failure<?>> execute(Iterator<String> lines);
    }

    private SourceFileProcessor() {}

    static ExitCode process(Path sourceFile, Operation operation, PrintStream err) {
        if (!FileArguments.requireExistingRegularFile(sourceFile, err)) {
            return ExitCode.USAGE_ERROR;
        }

        try {
            long totalBytes = Files.size(sourceFile);
            try (BufferedReader reader =
                    Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
                Iterator<String> lines =
                        new ProgressReportingIterator(reader.lines().iterator(), totalBytes, err);
                List<Failure<?>> failures = operation.execute(lines);
                new FailureReporter(err).report(failures);
                return failures.isEmpty() ? ExitCode.SUCCESS : ExitCode.SOURCE_ERRORS;
            }
        } catch (IOException e) {
            err.println("No se pudo leer '" + sourceFile + "': " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
    }
}
