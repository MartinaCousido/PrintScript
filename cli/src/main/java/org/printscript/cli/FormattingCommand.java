package org.printscript.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// formatea el .ps con el módulo formatter, aplicando las reglas del --config; escribe
// el resultado por stdout o, con --output, a un archivo (nunca pisa el fuente)
@Command(
        name = "formatting",
        aliases = {"format", "fmt"},
        description = "Formatea un archivo PrintScript con el formatter")
final class FormattingCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a formatear")
    private Path sourceFile;

    @Option(
            names = {"-c", "--config"},
            description = "Archivo de configuración YAML/JSON de formato")
    private Path configFile;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "SALIDA",
            description = "Escribe el código formateado en este archivo en vez de por stdout")
    private Path outputFile;

    @Override
    public Integer call() {
        return Formatting.run(sourceFile, configFile, outputFile, System.out, System.err).code();
    }
}
