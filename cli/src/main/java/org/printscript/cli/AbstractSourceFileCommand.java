package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.printscript.common.Failure;
import picocli.CommandLine.Parameters;

// abre el archivo fuente, arma el iterator de líneas con progreso y delega en la
// operación concreta (execution/validation); cero lógica de lenguaje acá
abstract class AbstractSourceFileCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a procesar")
    private Path sourceFile;

    protected abstract List<Failure<?>> execute(Iterator<String> lines);

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public final Integer call() {
        if (!FileArguments.requireExistingRegularFile(sourceFile, System.err)) {
            return ExitCode.USAGE_ERROR.code();
        }

        try {
            long totalBytes = Files.size(sourceFile);
            try (BufferedReader reader =
                    Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
                Iterator<String> lines =
                        new ProgressReportingIterator(
                                reader.lines().iterator(), totalBytes, System.err);
                List<Failure<?>> failures = execute(lines);
                new FailureReporter(System.err).report(failures);
                return failures.isEmpty() ? ExitCode.SUCCESS.code() : ExitCode.SOURCE_ERRORS.code();
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer '" + sourceFile + "': " + e.getMessage());
            return ExitCode.USAGE_ERROR.code();
        }
    }
}
