package org.printscript.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.runner.FormattingRunner;

// Orquesta la operación de formateo para cli: carga las reglas del --config (o usa las
// de por defecto), corre el FormattingRunner y escribe el código formateado a un
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
        return formatToFile(lines, configuration, outputFile, console, err, version);
    }

    private static ExitCode formatToConsole(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            PrintStream console,
            PrintStream err,
            Version version) {
        // No se cierra la consola: el resto del proceso sigue usándola.
        Writer destination = new OutputStreamWriter(console, StandardCharsets.UTF_8);
        List<Failure<?>> failures =
                new FormattingRunner().format(lines, configuration, destination, version);
        return finish(failures, err);
    }

    private static ExitCode formatToFile(
            Iterator<String> lines,
            FormatterConfiguration configuration,
            Path outputFile,
            PrintStream console,
            PrintStream err,
            Version version) {
        ExitCode result =
                FileOutputs.write(
                        outputFile,
                        err,
                        destination ->
                                finish(
                                        new FormattingRunner()
                                                .format(lines, configuration, destination, version),
                                        err));
        if (result == ExitCode.SUCCESS) {
            console.println("Código formateado guardado en " + outputFile);
        }
        return result;
    }

    private static ExitCode finish(List<Failure<?>> failures, PrintStream err) {
        new FailureReporter(err).report(failures);
        return failures.isEmpty() ? ExitCode.SUCCESS : ExitCode.SOURCE_ERRORS;
    }
}
