package org.printscript.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// analiza estáticamente el .ps con el módulo linter, aplicando las reglas del
// archivo de configuración (--config); sin config, ninguna regla está prendida
@Command(
        name = "analyzing",
        aliases = {"analyze", "lint"},
        description = "Analiza estáticamente un archivo PrintScript con el linter")
final class AnalyzingCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a analizar")
    private Path sourceFile;

    @Option(
            names = {"-c", "--config"},
            description = "Archivo de configuración YAML/JSON de reglas de análisis")
    private Path configFile;

    @Override
    public Integer call() {
        return LintAnalysis.run(sourceFile, configFile, System.err).code();
    }
}
