package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidationCommandEndToEndTest {

    @TempDir Path tempDir;

    @Test
    void validProgramExitsZeroWithoutExecutingAnything() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("validation", file.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().isEmpty());
    }

    @Test
    void syntaxErrorIsReportedWithFullRangeWithoutExecutingAnything() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "syntax-error-missing-colon.ps");

        CliTestSupport.Result result = CliTestSupport.run("validation", file.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Expected ':' after the variable name"));
        assertTrue(result.stderr().contains("línea 1, columna 7"));
        assertTrue(result.stderr().contains("línea 1, columna 12"));
        assertTrue(result.stdout().isEmpty());
    }
}
