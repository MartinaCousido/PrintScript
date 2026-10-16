package org.printscript.cli;

import java.util.concurrent.Callable;
import org.printscript.common.Version;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        name = "printscript",
        subcommands = {
            ExecutionCommand.class,
            ValidationCommand.class,
            FormattingCommand.class,
            AnalyzingCommand.class
        },
        description = "CLI de PrintScript: ejecuta, valida, formatea o analiza un archivo .ps")
public final class RootCommand implements Callable<Integer> {

    @Option(
            names = {"-v", "--version"},
            converter = LanguageVersion.Converter.class,
            defaultValue = "1.0",
            scope = ScopeType.INHERIT,
            description = "Versión del lenguaje a interpretar (1.0 o 1.1)")
    private LanguageVersion version;

    Version language() {
        return version.domain();
    }

    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            scope = ScopeType.INHERIT,
            description = "Muestra este mensaje de ayuda")
    private boolean helpRequested;

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public Integer call() {
        System.err.println(
                "Falta indicar una operación: execution, validation, formatting o analyzing.");
        return ExitCode.USAGE_ERROR.code();
    }
}
