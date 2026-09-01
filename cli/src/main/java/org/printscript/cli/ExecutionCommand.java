package org.printscript.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "execution",
        aliases = {"exec"},
        description = "Ejecuta un archivo PrintScript, imprimiendo su salida por stdout")
final class ExecutionCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "ARCHIVO", description = "Archivo .ps a ejecutar")
    private Path sourceFile;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "SALIDA",
            description = "Guarda la salida del programa en este archivo, además de mostrarla")
    private Path outputFile;

    @Override
    public Integer call() {
        return ExecutionOutput.run(sourceFile, outputFile, System.out, System.err).code();
    }
}
