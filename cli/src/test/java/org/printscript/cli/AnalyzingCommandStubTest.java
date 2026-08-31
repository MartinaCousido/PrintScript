package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzingCommandStubTest {

    @TempDir Path tempDir;

    @Test
    void reportsNotImplementedYet() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("analyzing", file.toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.stderr().contains("no está implementado"));
    }

    @Test
    void lintAliasBehavesLikeAnalyzing() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("lint", file.toString());

        assertEquals(3, result.exitCode());
    }
}
