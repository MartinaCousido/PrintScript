package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzingCommandEndToEndTest {

    @TempDir Path tempDir;

    private static final String ANALYZING = "analyzing";

    private static final String CONFIG_CAMEL_Y_PRINTLN =
            "{ \"identifier_format\": \"camel case\","
                    + " \"mandatory-variable-or-literal-in-println\": true }";

    private Path write(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void cleanFileWithConfigReportsNoViolationsAndExitsZero() throws IOException {
        Path source = write("limpio.ps", "let nombre: string = \"Joe\";\nprintln(nombre);\n");
        Path config = write("reglas.json", CONFIG_CAMEL_Y_PRINTLN);

        CliTestSupport.Result result =
                CliTestSupport.run(ANALYZING, source.toString(), "-c", config.toString());

        assertEquals(0, result.exitCode());
    }

    @Test
    void snakeCaseIdentifierIsReportedAsViolationWithExitOne() throws IOException {
        Path source = write("snake.ps", "let last_name: string = \"Doe\";\n");
        Path config = write("reglas.json", CONFIG_CAMEL_Y_PRINTLN);

        CliTestSupport.Result result =
                CliTestSupport.run("lint", source.toString(), "-c", config.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("lint:"), result.stderr());
        assertTrue(result.stderr().contains("last_name"), result.stderr());
    }

    @Test
    void withoutConfigNoRuleIsActiveSoThereAreNoViolations() throws IOException {
        // last_name incumpliría camelCase, pero sin config ninguna regla está prendida
        Path source = write("snake.ps", "let last_name: string = \"Doe\";\n");

        CliTestSupport.Result result = CliTestSupport.run(ANALYZING, source.toString());

        assertEquals(0, result.exitCode());
    }

    @Test
    void syntaxErrorIsReportedAsFailureWithExitOne() throws IOException {
        Path source = write("roto.ps", "let x number = 1;\n");
        Path config = write("reglas.json", CONFIG_CAMEL_Y_PRINTLN);

        CliTestSupport.Result result =
                CliTestSupport.run(ANALYZING, source.toString(), "-c", config.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("error:"), result.stderr());
    }

    @Test
    void configWithoutExtensionIsRejectedAsUsageError() throws IOException {
        Path source = write("limpio.ps", "println(1);\n");
        Path config = write("reglas", CONFIG_CAMEL_Y_PRINTLN);

        CliTestSupport.Result result =
                CliTestSupport.run(ANALYZING, source.toString(), "-c", config.toString());

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("configuración"), result.stderr());
    }

    @Test
    void missingConfigFileIsRejectedAsUsageError() throws IOException {
        Path source = write("limpio.ps", "println(1);\n");

        CliTestSupport.Result result =
                CliTestSupport.run(
                        ANALYZING,
                        source.toString(),
                        "-c",
                        tempDir.resolve("no-existe.json").toString());

        assertEquals(2, result.exitCode());
    }
}
