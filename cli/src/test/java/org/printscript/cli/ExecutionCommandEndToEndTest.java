package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutionCommandEndToEndTest {

    @TempDir Path tempDir;

    @Test
    void validProgramPrintsExpectedOutputAndExitsZero() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("execution", file.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Joe Doe"));
        assertFalse(result.stderr().contains("error:"));
    }

    @Test
    void execAliasBehavesLikeExecution() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("exec", file.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Joe Doe"));
    }
}
