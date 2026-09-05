package org.printscript.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Version;
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

    static ExitCode run(Path sourceFile, Path configFile, PrintStream err, Version version) {
        LinterConfiguration configuration;
        try {
            configuration = loadConfiguration(configFile);
        } catch (IOException | IllegalArgumentException e) {
            err.println("No se pudo leer la configuración: " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
        return SourceFileProcessor.read(
                sourceFile,
                lines -> report(new AnalyzingRunner().analyze(lines, configuration, version), err),
                err);
    }

    // sin archivo de configuración, ninguna regla está prendida (todas se apagan si
    // su clave no aparece), así que el análisis no reporta violaciones
    private static LinterConfiguration loadConfiguration(Path configFile) throws IOException {
        return LinterConfiguration.from(ConfigurationFiles.read(configFile));
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
