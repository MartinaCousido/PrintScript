package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.printscript.common.Version;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

// Ejecuta un archivo PrintScript, opcionalmente guardando la salida de sus println en
// un archivo además de mostrarla por consola
final class ExecutionOutput {

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
        if (!FileArguments.requireExistingRegularFile(sourceFile, err)) {
            return ExitCode.USAGE_ERROR;
        }
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            OutputPrinter printer =
                    new TeeOutputPrinter(
                            new ConsoleOutputPrinter(console), new WriterOutputPrinter(writer));
            ExitCode code = execute(sourceFile, printer, err, version, reader);
            console.println("Salida guardada en " + outputFile);
            return code;
        } catch (IOException e) {
            err.println("No se pudo escribir la salida en '" + outputFile + "': " + e.getMessage());
            return ExitCode.USAGE_ERROR;
        }
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
        String base = name.endsWith(".ps") ? name.substring(0, name.length() - 3) : name;
        return absolute.resolveSibling(base + ".out.txt");
    }
}
