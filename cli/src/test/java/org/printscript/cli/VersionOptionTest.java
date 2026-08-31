package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionOptionTest {

    @TempDir Path tempDir;

    @Test
    void unsupportedVersionFailsWithUsageError() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result =
                CliTestSupport.run("--version", "2.0", "execution", file.toString());

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("1.0"));
    }

    @Test
    void explicitSupportedVersionBehavesLikeDefault() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result =
                CliTestSupport.run("--version", "1.0", "execution", file.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Joe Doe"));
    }
}
