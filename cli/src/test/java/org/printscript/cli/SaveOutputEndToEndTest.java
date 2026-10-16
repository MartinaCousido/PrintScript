package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SaveOutputEndToEndTest {

    private static final String EXECUTION = "execution";

    @TempDir Path tempDir;

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void executionWithOutputOptionSavesPrintsAndStillShowsThem() throws IOException {
        Path source = write("hola.ps", "println(\"hola\");\nprintln(\"chau\");\n");
        Path output = tempDir.resolve("salida.txt");

        CliTestSupport.Result result =
                CliTestSupport.run(EXECUTION, source.toString(), "-o", output.toString());

        assertEquals(0, result.exitCode());
        assertEquals("hola\nchau\n", result.stdout());
        assertTrue(result.stderr().contains("Salida guardada en"), result.stderr());
        assertTrue(Files.exists(output), "crea el archivo de salida");
        String saved = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(saved.contains("hola") && saved.contains("chau"), saved);
    }

    @Test
    void missingSourceDoesNotCreateOutputFile() throws IOException {
        Path output = tempDir.resolve("salida.txt");

        CliTestSupport.Result result =
                CliTestSupport.run(
                        EXECUTION,
                        tempDir.resolve("no-existe.ps").toString(),
                        "-o",
                        output.toString());

        assertEquals(2, result.exitCode());
        assertFalse(Files.exists(output), "no deja un .txt vacío si el fuente no existe");
    }

    @Test
    void outputCannotOverwriteTheSourceOrAnAliasOfIt() throws IOException {
        String program = "println(\"keep me\");\n";
        Path source = write("original.ps", program);
        Path alias = tempDir.resolve("alias.ps");
        Files.createSymbolicLink(alias, source);
        for (Path output : new Path[] {source, alias}) {
            CliTestSupport.Result result =
                    CliTestSupport.run(EXECUTION, source.toString(), "-o", output.toString());
            assertEquals(2, result.exitCode());
            assertEquals(program, Files.readString(source));
        }
    }

    @Test
    void executionErrorsPreserveThePreviousOutput() throws IOException {
        Path source = write("invalid.ps", "println(\"partial\");\nprintln(missing);\n");
        Path output = write("previous.txt", "previous output");
        CliTestSupport.Result result =
                CliTestSupport.run(EXECUTION, source.toString(), "-o", output.toString());
        assertEquals(1, result.exitCode());
        assertEquals("previous output", Files.readString(output));
        assertFalse(result.stdout().contains("Salida guardada"));
    }
}
