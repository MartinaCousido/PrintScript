package org.printscript.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;

// helper de tests: corre la CLI capturando stdout/stderr, y copia los .ps de
// classpath a un archivo real, ya que cli opera sobre Path del filesystem
final class CliTestSupport {

    private CliTestSupport() {}

    record Result(int exitCode, String stdout, String stderr) {}

    static Path copyResource(Path dir, String resourceName) throws IOException {
        Path target = dir.resolve(resourceName);
        try (InputStream in =
                CliTestSupport.class.getResourceAsStream("/examples/" + resourceName)) {
            Files.copy(in, target);
        }
        return target;
    }

    // originalOut/originalErr son alias de System.out/System.err para restaurarlos
    // al final: cerrarlos rompería la salida estándar del resto de la JVM. PMD no
    // distingue eso de un recurso propio sin cerrar.
    @SuppressWarnings("PMD.CloseResource")
    static Result run(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
                PrintStream capturedErr =
                        new PrintStream(errBuffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            int exitCode = new CommandLine(new RootCommand()).execute(args);
            return new Result(
                    exitCode,
                    outBuffer.toString(StandardCharsets.UTF_8),
                    errBuffer.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
