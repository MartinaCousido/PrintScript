package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FormattingCommandStubTest {

    @TempDir Path tempDir;

    @Test
    void reportsNotImplementedYet() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("formatting", file.toString());

        assertEquals(3, result.exitCode());
        assertTrue(result.stderr().contains("no está implementado"));
    }

    @Test
    void acceptsConfigOptionWithoutFailingToParse() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");
        Path config = tempDir.resolve("rules.yaml");
        Files.writeString(config, "indent: 2");

        CliTestSupport.Result result =
                CliTestSupport.run("formatting", file.toString(), "--config", config.toString());

        assertEquals(3, result.exitCode());
    }

    @Test
    void fmtAliasBehavesLikeFormatting() throws IOException {
        Path file = CliTestSupport.copyResource(tempDir, "valid-hello.ps");

        CliTestSupport.Result result = CliTestSupport.run("fmt", file.toString());

        assertEquals(3, result.exitCode());
    }
}
