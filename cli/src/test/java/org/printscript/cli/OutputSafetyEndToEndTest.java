package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OutputSafetyEndToEndTest {

    private static final String EXECUTION = "execution";
    private static final String FORMATTING = "formatting";
    private static final String SOURCE_NAME = "programa.ps";
    private static final String PROGRAM = "println(\"hola\");\n";

    @TempDir Path dir;

    @ParameterizedTest
    @ValueSource(strings = {EXECUTION, FORMATTING})
    void rejectsSourceAsOutputWithoutChangingIt(String command) throws IOException {
        Path source = Files.writeString(dir.resolve(SOURCE_NAME), PROGRAM);

        assertProtected(command, source, source);
    }

    @ParameterizedTest
    @ValueSource(strings = {EXECUTION, FORMATTING})
    void rejectsNormalizedAliasOfSource(String command) throws IOException {
        Path source = Files.writeString(dir.resolve(SOURCE_NAME), PROGRAM);
        Path folder = Files.createDirectory(dir.resolve("subcarpeta"));

        assertProtected(command, source, folder.resolve("../programa.ps"));
    }

    @ParameterizedTest
    @ValueSource(strings = {EXECUTION, FORMATTING})
    void rejectsSymbolicLinkToSource(String command) throws IOException {
        Path source = Files.writeString(dir.resolve(SOURCE_NAME), PROGRAM);
        Path alias = Files.createSymbolicLink(dir.resolve("alias.ps"), source);

        assertProtected(command, source, alias);
    }

    @ParameterizedTest
    @ValueSource(strings = {EXECUTION, FORMATTING})
    void rejectsHardLinkToSource(String command) throws IOException {
        Path source = Files.writeString(dir.resolve(SOURCE_NAME), PROGRAM);
        Path alias = Files.createLink(dir.resolve("alias.ps"), source);

        assertProtected(command, source, alias);
    }

    private static void assertProtected(String command, Path source, Path output)
            throws IOException {
        CliTestSupport.Result result =
                CliTestSupport.run(command, source.toString(), "-o", output.toString());

        assertEquals(2, result.exitCode());
        assertEquals(PROGRAM, Files.readString(source));
        assertTrue(result.stderr().contains("distinto del fuente"), result.stderr());
    }
}
