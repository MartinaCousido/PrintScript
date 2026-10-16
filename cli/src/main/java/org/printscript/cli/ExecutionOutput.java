package org.printscript.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.printscript.common.Version;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

// Ejecuta un archivo PrintScript, opcionalmente guardando la salida de sus println en
// un archivo además de mostrarla por consola
final class ExecutionOutput {

    private static final String SOURCE_EXTENSION = ".ps";
    private static final String OUTPUT_EXTENSION = ".out.txt";

    private ExecutionOutput() {}

    static ExitCode run(
            Path sourceFile,
            Path outputFile,
            PrintStream console,
            PrintStream err,
            Version version,
            BufferedReader reader) {
        if (outputFile == null) {
            return execute(sourceFile, new ConsoleOutputPrinter(console), err, version, reader);
        }
        // se valida el fuente antes de crear el archivo de salida para no dejar un .txt
        // vacío cuando el .ps no existe
        if (!FileArguments.requireExistingRegularFile(sourceFile, err)
                || !FileArguments.requireDistinctOutput(sourceFile, outputFile, err)) {
            return ExitCode.USAGE_ERROR;
        }
        ExitCode result =
                FileOutputs.write(
                        outputFile,
                        err,
                        writer -> {
                            OutputPrinter printer =
                                    new TeeOutputPrinter(
                                            new ConsoleOutputPrinter(console),
                                            new WriterOutputPrinter(writer));
                            return execute(sourceFile, printer, err, version, reader);
                        });
        if (result == ExitCode.SUCCESS) {
            console.println("Salida guardada en " + outputFile);
        }
        return result;
    }

    private static ExitCode execute(
            Path sourceFile,
            OutputPrinter printer,
            PrintStream err,
            Version version,
            BufferedReader reader) {
        RuntimePorts ports =
                new RuntimePorts(
                        printer,
                        new ConsoleInputProvider(standardInput(reader)),
                        new SystemEnvProvider());
        return SourceFileProcessor.process(
                sourceFile, lines -> new ExecutionRunner().run(lines, ports, version), err);
    }

    private static BufferedReader standardInput(BufferedReader reader) {
        return reader != null
                ? reader
                : new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    // deriva el archivo de salida junto al fuente: ejemplo.ps -> ejemplo.out.txt
    static Path defaultOutputFor(Path sourceFile) {
        Path absolute = sourceFile.toAbsolutePath();
        String name = absolute.getFileName().toString();
        String base =
                name.endsWith(SOURCE_EXTENSION)
                        ? name.substring(0, name.length() - SOURCE_EXTENSION.length())
                        : name;
        return absolute.resolveSibling(base + OUTPUT_EXTENSION);
    }
}
