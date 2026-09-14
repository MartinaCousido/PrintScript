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
import java.io.Writer;
import java.util.List;

import org.printscript.config.ConfigurationReader;
import org.printscript.config.JsonConfigurationMapper;
import org.printscript.config.RulesConfiguration;
import org.printscript.config.YamlConfigurationMapper;
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

    private static final ConfigurationReader CONFIGURATION =
            new ConfigurationReader(
                    List.of(new JsonConfigurationMapper(), new YamlConfigurationMapper()));

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
            try {
                report(new ExecutionRunner().run(lines(src), ports, versionOf(version)), handler);
            } catch (OutOfMemoryError exhausted) {
                handler.reportError(exhausted.getMessage());
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
