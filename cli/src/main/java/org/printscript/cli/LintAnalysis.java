package org.printscript.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.config.ConfigurationReader;
import org.printscript.config.JsonConfigurationMapper;
import org.printscript.config.YamlConfigurationMapper;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runner.AnalysisReport;
import org.printscript.runner.AnalyzingRunner;

// Orquesta la operación de análisis estático para cli: carga la configuración de
// reglas desde el archivo del usuario (o usa una vacía si no la pasó), corre el
// AnalyzingRunner sobre el .ps y reporta violaciones y errores de sintaxis. Lo usan
// tanto AnalyzingCommand como la interfaz interactiva.
final class LintAnalysis {

    private LintAnalysis() {}

    static ExitCode run(Path sourceFile, Path configFile, PrintStream err) {
        LinterConfiguration configuration;
        try {
            configuration = loadConfiguration(configFile);
        } catch (IOException | IllegalArgumentException e) {
            err.println("No se pudo leer la configuración: " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
        return SourceFileProcessor.read(
                sourceFile,
                lines -> report(new AnalyzingRunner().analyze(lines, configuration), err),
                err);
    }

    // sin archivo de configuración, ninguna regla está prendida (todas se apagan si
    // su clave no aparece), así que el análisis no reporta violaciones
    private static LinterConfiguration loadConfiguration(Path configFile) throws IOException {
        if (configFile == null) {
            return new LinterConfiguration(Optional.empty(), false);
        }
        if (!Files.isRegularFile(configFile)) {
            throw new IllegalArgumentException(
                    "el archivo de configuración '" + configFile + "' no existe");
        }
        String format = formatOf(configFile);
        try (InputStream source = Files.newInputStream(configFile)) {
            ConfigurationReader reader =
                    new ConfigurationReader(
                            List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));
            return LinterConfiguration.from(reader.read(source, format));
        }
    }

    private static String formatOf(Path configFile) {
        String name = configFile.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException(
                    "el archivo de configuración no tiene extensión (.json/.yaml/.yml): "
                            + configFile);
        }
        return name.substring(dot + 1);
    }

    private static ExitCode report(AnalysisReport analysisReport, PrintStream err) {
        new FailureReporter(err).report(analysisReport.failures());
        for (Violation violation : analysisReport.violations()) {
            err.println(format(violation));
        }
        boolean clean =
                analysisReport.failures().isEmpty() && analysisReport.violations().isEmpty();
        return clean ? ExitCode.SUCCESS : ExitCode.SOURCE_ERRORS;
    }

    private static String format(Violation violation) {
        Range range = violation.range();
        Position start = range.start();
        Position end = range.end();
        return "lint: "
                + violation.message()
                + " (línea "
                + start.line()
                + ", columna "
                + start.column()
                + " a línea "
                + end.line()
                + ", columna "
                + end.column()
                + ")";
    }
}
