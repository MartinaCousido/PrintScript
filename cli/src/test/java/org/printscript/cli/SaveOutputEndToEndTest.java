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
                CliTestSupport.run("execution", source.toString(), "-o", output.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("hola"), "muestra la salida por stdout");
        assertTrue(Files.exists(output), "crea el archivo de salida");
        String saved = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(saved.contains("hola") && saved.contains("chau"), saved);
    }

    @Test
    void missingSourceDoesNotCreateOutputFile() throws IOException {
        Path output = tempDir.resolve("salida.txt");

        CliTestSupport.Result result =
                CliTestSupport.run(
                        "execution",
                        tempDir.resolve("no-existe.ps").toString(),
                        "-o",
                        output.toString());

        assertEquals(2, result.exitCode());
        assertFalse(Files.exists(output), "no deja un .txt vacío si el fuente no existe");
    }
}
