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

class FileOutputsTest {
    @TempDir Path directory;

    @Test
    void aWriteFailurePreservesTheDestinationAndRemovesTheTemporaryFile() throws IOException {
        Path output = directory.resolve("result.txt");
        Files.writeString(output, "keep");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ExitCode result =
                FileOutputs.write(
                        output,
                        new PrintStream(errors, true, StandardCharsets.UTF_8),
                        writer -> {
                            try {
                                writer.write("partial");
                            } catch (IOException exception) {
                                throw new UncheckedIOException(exception);
                            }
                            throw new UncheckedIOException(new IOException("device failure"));
                        });
        assertEquals(ExitCode.USAGE_ERROR, result);
        assertEquals("keep", Files.readString(output));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("device failure"));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
