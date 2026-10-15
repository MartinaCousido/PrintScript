package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InactiveRulesEndToEndTest {

    @TempDir Path dir;

    @ParameterizedTest
    @ValueSource(strings = {"formatting", "analyzing"})
    void explainsMissingConfiguration(String command) throws IOException {
        Path source = Files.writeString(dir.resolve("programa.ps"), "let a:number=12;\n");

        CliTestSupport.Result result = CliTestSupport.run(command, source.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("no hay reglas"), result.stderr());
        assertTrue(result.stderr().contains("--config"), result.stderr());
    }

    @ParameterizedTest
    @ValueSource(strings = {"formatting", "analyzing"})
    void explainsEmptyConfiguration(String command) throws IOException {
        Path source = Files.writeString(dir.resolve("programa.ps"), "let a:number=12;\n");
        Path config = Files.writeString(dir.resolve("reglas.json"), "{}");

        CliTestSupport.Result result =
                CliTestSupport.run(command, source.toString(), "-c", config.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("no hay reglas"), result.stderr());
    }

    @Test
    void analyzingStillReportsSyntaxErrorsWithoutRules() throws IOException {
        Path source = Files.writeString(dir.resolve("programa.ps"), "let a number = 12;\n");

        CliTestSupport.Result result = CliTestSupport.run("analyzing", source.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("error:"), result.stderr());
        assertTrue(result.stderr().contains("solo se revisa la sintaxis"), result.stderr());
    }
}
