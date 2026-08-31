package org.printscript.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// el módulo formatter todavía no existe (ver docs/arquitectura.md); esta operación
// queda expuesta en el choice cerrado de 4 que pide la consigna, pero falla de
// forma clara y controlada en vez de simularse o silenciarse
@Command(
        name = "formatting",
        aliases = {"format", "fmt"},
        description = "Formatea un archivo PrintScript (no implementado todavía)")
final class FormattingCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a formatear")
    private Path sourceFile;

    @Option(
            names = {"-c", "--config"},
            description = "Archivo de configuración YAML/JSON de formato")
    private Path configFile;

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public Integer call() {
        if (!FileArguments.requireExistingRegularFile(sourceFile, System.err)) {
            return ExitCode.USAGE_ERROR.code();
        }
        System.err.println(
                "Formatting no está implementado todavía: el módulo formatter no existe "
                        + "(ver docs/arquitectura.md).");
        return ExitCode.UNSUPPORTED_OPERATION.code();
    }
}
