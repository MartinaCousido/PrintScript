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

// abre el archivo fuente, arma el iterator de líneas con progreso y delega en el
// caller. Es el único camino por el que cli lee un .ps del filesystem: lo comparten
// los subcomandos (AbstractSourceFileCommand, AnalyzingCommand) y la interfaz
// interactiva (InteractiveShell), así todos se comportan idéntico.
final class SourceFileProcessor {

    // operación que produce fallas (execution/validation): read() se encarga de
    // reportarlas y de traducir "hubo fallas" al exit code
    @FunctionalInterface
    interface Operation {
        List<Failure<?>> execute(Iterator<String> lines);
    }

    // operación que produce su propio exit code (analyzing, que además reporta
    // violaciones); recibe el iterator de líneas ya decorado con progreso
    @FunctionalInterface
    interface LineConsumer {
        ExitCode consume(Iterator<String> lines);
    }

    private SourceFileProcessor() {}

    static ExitCode process(Path sourceFile, Operation operation, PrintStream err) {
        return read(
                sourceFile,
                lines -> {
                    List<Failure<?>> failures = operation.execute(lines);
                    new FailureReporter(err).report(failures);
                    return failures.isEmpty() ? ExitCode.SUCCESS : ExitCode.SOURCE_ERRORS;
                },
                err);
    }

    static ExitCode read(Path sourceFile, LineConsumer consumer, PrintStream err) {
        if (!FileArguments.requireExistingRegularFile(sourceFile, err)) {
            return ExitCode.USAGE_ERROR;
        }

        try {
            long totalBytes = Files.size(sourceFile);
            try (BufferedReader reader =
                    Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
                Iterator<String> lines =
                        new ProgressReportingIterator(reader.lines().iterator(), totalBytes, err);
                return consumer.consume(lines);
            }
        } catch (IOException e) {
            err.println("No se pudo leer '" + sourceFile + "': " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
    }
}
