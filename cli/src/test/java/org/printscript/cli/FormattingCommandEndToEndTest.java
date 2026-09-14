package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FormattingCommandEndToEndTest {

    @TempDir Path tempDir;

    private static final String COMANDO = "formatting";
    private static final String CONFIG_ESPACIADO =
            "{ \"enforce-spacing-after-colon-in-declaration\": true,"
                    + " \"enforce-spacing-around-equals\": true }";

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void formatsToStdoutApplyingConfigRules() throws IOException {
        Path source = write("feo.ps", "let a:number=12;\n");
        Path config = write("reglas.json", CONFIG_ESPACIADO);

        CliTestSupport.Result result =
                CliTestSupport.run(COMANDO, source.toString(), "-c", config.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("let a: number = 12;"), result.stdout());
    }

    @Test
    void writesFormattedCodeToOutputFileWhenRequested() throws IOException {
        Path source = write("feo.ps", "let a:number=12;\n");
        Path config = write("reglas.json", CONFIG_ESPACIADO);
        Path output = tempDir.resolve("lindo.ps");

        CliTestSupport.Result result =
                CliTestSupport.run(
                        "fmt", source.toString(), "-c", config.toString(), "-o", output.toString());

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(output), "escribe el archivo de salida");
        assertTrue(
                Files.readString(output, StandardCharsets.UTF_8).contains("let a: number = 12;"),
                "el archivo tiene el código formateado");
    }

    // formatting no parsea: reescribe el espacio entre tokens. Un programa que no compila
    // igual se acomoda, que es cuando más falta hace. Validar es trabajo de `validation`.
    @Test
    void formatsAProgramThatDoesNotParseAndExitsZero() throws IOException {
        Path source = write("roto.ps", "let x number = 1;\n");

        CliTestSupport.Result result = CliTestSupport.run(COMANDO, source.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("let x number = 1;"), result.stdout());
    }

    @Test
    void reportsASymbolTheLexerCannotReadWithExitOne() throws IOException {
        Path source = write("ilegible.ps", "let a: number = #;\n");

        CliTestSupport.Result result = CliTestSupport.run(COMANDO, source.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("error:"), result.stderr());
    }

    @Test
    void badConfigExtensionIsUsageError() throws IOException {
        Path source = write("feo.ps", "let a:number=12;\n");
        Path config = write("reglas", CONFIG_ESPACIADO);

        CliTestSupport.Result result =
                CliTestSupport.run(COMANDO, source.toString(), "-c", config.toString());

        assertEquals(2, result.exitCode());
    }
}
