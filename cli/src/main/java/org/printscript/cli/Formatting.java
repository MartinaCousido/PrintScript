package org.printscript.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.runner.FormattingRunner;

// Orquesta la operación de formateo para cli: carga las reglas del --config,
// corre el FormattingRunner y escribe el código formateado a un
// archivo (--output) o por consola.
final class Formatting {

    private Formatting() {}

    static ExitCode run(
            Path sourceFile,
            Path configFile,
            Path outputFile,
            PrintStream console,
            PrintStream err,
            Version version) {
        if (!FileArguments.requireExistingRegularFile(sourceFile, err)
                || !FileArguments.requireDistinctOutput(sourceFile, outputFile, err)) {
            return ExitCode.USAGE_ERROR;
        }
        FormatterConfiguration configuration;
        try {
            configuration = FormatterConfiguration.from(ConfigurationFiles.read(configFile));
        } catch (IOException | IllegalArgumentException e) {
            err.println("No se pudo leer la configuración: " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
        if (configuration.equals(FormatterConfiguration.none())) {
            err.println("Aviso: no hay reglas de formato activas. Usá --config para activarlas.");
        }
        return SourceFileProcessor.read(
                sourceFile,
                lines -> format(lines, configuration, outputFile, console, err, version),
                err);
    }

    private static ExitCode format(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            Path outputFile,
            PrintStream console,
            PrintStream err,
            Version version) {
        if (outputFile == null) {
            return formatToConsole(lines, configuration, console, err, version);
        }
        return formatToFile(lines, configuration, outputFile, err, version);
    }

    private static ExitCode formatToConsole(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            PrintStream console,
            PrintStream err,
            Version version) {
        // No se cierra la consola: el resto del proceso sigue usándola.
        Writer destination = new OutputStreamWriter(console, StandardCharsets.UTF_8);
        boolean clean =
                new FormattingRunner()
                        .format(
                                lines,
                                configuration,
                                destination,
                                version,
                                new FailureReporter(err)::report);
        return finish(clean);
    }

    private static ExitCode formatToFile(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            Path outputFile,
            PrintStream err,
            Version version) {
        ExitCode result =
                FileOutputs.write(
                        outputFile,
                        err,
                        destination ->
                                finish(
                                        new FormattingRunner()
                                                .format(
                                                        lines,
                                                        configuration,
                                                        destination,
                                                        version,
                                                        new FailureReporter(err)::report)));
        if (result == ExitCode.SUCCESS) {
            err.println("Código formateado guardado en " + outputFile);
        }
        return result;
    }

    private static ExitCode finish(boolean clean) {
        return clean ? ExitCode.SUCCESS : ExitCode.SOURCE_ERRORS;
    }
}
