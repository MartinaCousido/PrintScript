package implementation;

import static implementation.PrintScriptAdapters.envPort;
import static implementation.PrintScriptAdapters.inputPort;
import static implementation.PrintScriptAdapters.lines;
import static implementation.PrintScriptAdapters.report;
import static implementation.PrintScriptAdapters.versionOf;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

import java.io.InputStream;
import java.lang.ref.Reference;

import org.printscript.config.ConfigurationReader;
import org.printscript.config.RulesConfiguration;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.linter.Violation;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runner.AnalysisReport;
import org.printscript.runner.AnalyzingRunner;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runner.FormattingRunner;
import org.printscript.runtime.RuntimePorts;

// Conecta PrintScript con el TCK.
//
// Las tres operaciones entran por las fachadas del módulo runner, que es el punto de
// entrada programático de la implementación: el mismo que usa la CLI. Acá no hay lógica
// de lenguaje, solo traducción de vocabularios.
public class CustomImplementationFactory implements PrintScriptFactory {

    // El handler puede necesitar memoria para guardar el diagnóstico después de un OOM.
    // Se reservan 4 KiB durante la ejecución y se libera antes de reportar el error.
    private static final int ERROR_REPORTING_RESERVE_BYTES = 4096;

    private static final ConfigurationReader CONFIGURATION =
            ConfigurationReader.standard();

    @Override
    public PrintScriptInterpreter interpreter() {
        return (src, version, emitter, handler, provider) -> {
            PrintScriptAdapters.EmittingPrinter printer =
                    new PrintScriptAdapters.EmittingPrinter(emitter);
            RuntimePorts ports =
                    new RuntimePorts(printer, inputPort(provider, printer), envPort());
            // Si el consumidor de la salida se queda sin memoria, el error llega desde
            // adentro del bucle de ejecución: el TCK lo espera reportado como un error más
            // y no como una JVM que se cae. Es el único caso en el que atrapar un Error en
            // vez de una Exception es lo correcto.
            byte[] errorReportingReserve = new byte[ERROR_REPORTING_RESERVE_BYTES];
            try {
                report(new ExecutionRunner().run(lines(src), ports, versionOf(version)), handler);
            } catch (OutOfMemoryError exhausted) {
                errorReportingReserve = null;
                handler.reportError(exhausted.getMessage());
            } finally {
                Reference.reachabilityFence(errorReportingReserve);
            }
        };
    }

    @Override
    public PrintScriptFormatter formatter() {
        return (src, version, config, writer) ->
                new FormattingRunner()
                        .format(
                                lines(src),
                                FormatterConfiguration.from(rules(config)),
                                writer,
                                versionOf(version));
    }

    @Override
    public PrintScriptLinter linter() {
        return (src, version, config, handler) -> {
            AnalysisReport reporte =
                    new AnalyzingRunner()
                            .analyze(
                                    lines(src),
                                    LinterConfiguration.from(rules(config)),
                                    versionOf(version));
            for (Violation violation : reporte.violations()) {
                handler.reportError(
                        PrintScriptAdapters.describe(violation.message(), violation.range()));
            }
            report(reporte.failures(), handler);
        };
    }

    // el TCK siempre pasa config.json; el lector soporta también yaml
    private static RulesConfiguration rules(InputStream config) {
        return CONFIGURATION.read(config, "json");
    }
}
