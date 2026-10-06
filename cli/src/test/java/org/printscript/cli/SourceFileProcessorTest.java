package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFileProcessorTest {
    @TempDir Path directory;

    @Test
    void anIoFailureWhileConsumingLinesBecomesAnExitCode() throws IOException {
        Path source = directory.resolve("source.ps");
        Files.writeString(source, "println(1);");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ExitCode result =
                SourceFileProcessor.read(
                        source,
                        lines -> {
                            lines.next();
                            throw new UncheckedIOException(new IOException("read failure"));
                        },
                        new PrintStream(errors, true, StandardCharsets.UTF_8));
        assertEquals(ExitCode.USAGE_ERROR, result);
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("read failure"));
    }
}
