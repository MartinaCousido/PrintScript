package org.printscript.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// los módulos linter/semantic todavía no existen (ver docs/arquitectura.md); esta
// operación queda expuesta en el choice cerrado de 4 que pide la consigna, pero
// falla de forma clara y controlada en vez de simularse o silenciarse
@Command(
        name = "analyzing",
        aliases = {"analyze", "lint"},
        description = "Analiza estáticamente un archivo PrintScript (no implementado todavía)")
final class AnalyzingCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a analizar")
    private Path sourceFile;

    @Option(
            names = {"-c", "--config"},
            description = "Archivo de configuración YAML/JSON de reglas de análisis")
    private Path configFile;

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public Integer call() {
        if (!FileArguments.requireExistingRegularFile(sourceFile, System.err)) {
            return ExitCode.USAGE_ERROR.code();
        }
        System.err.println(
                "Analyzing no está implementado todavía: los módulos linter/semantic no existen "
                        + "(ver docs/arquitectura.md).");
        return ExitCode.UNSUPPORTED_OPERATION.code();
    }
}
