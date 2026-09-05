package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Version11EndToEndTest {

    private static final String PROGRAMA = "programa.ps";
    private static final String FLAG_VERSION = "--version";
    private static final String V11 = "1.1";
    private static final String EJECUTAR = "execution";

    private static final String PROGRAMA_11 =
            """
            const saludo: string = "hola";
            let listo: boolean = true;
            if (listo) {
              println(saludo);
            } else {
              println("no");
            }
            """;

    private static Path write(Path dir, String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void version11RunsAProgramWithConstBooleanAndIf(@TempDir Path dir) throws IOException {
        Path source = write(dir, PROGRAMA, PROGRAMA_11);

        CliTestSupport.Result result =
                CliTestSupport.run(FLAG_VERSION, V11, EJECUTAR, source.toString());

        assertEquals(ExitCode.SUCCESS.code(), result.exitCode());
        assertTrue(result.stdout().contains("hola"));
    }

    @Test
    void thatSameProgramFailsInVersion10(@TempDir Path dir) throws IOException {
        Path source = write(dir, PROGRAMA, PROGRAMA_11);

        CliTestSupport.Result result = CliTestSupport.run(EJECUTAR, source.toString());

        assertEquals(ExitCode.SOURCE_ERRORS.code(), result.exitCode());
    }

    @Test
    void anUnsupportedVersionListsTheOnesThatDoExist(@TempDir Path dir) throws IOException {
        Path source = write(dir, PROGRAMA, "println(1);");

        CliTestSupport.Result result =
                CliTestSupport.run(FLAG_VERSION, "2.0", EJECUTAR, source.toString());

        assertEquals(ExitCode.USAGE_ERROR.code(), result.exitCode());
        assertTrue(result.stderr().contains("1.0, 1.1"), result.stderr());
    }

    @Test
    void version11AlsoReachesValidationFormattingAndAnalyzing(@TempDir Path dir)
            throws IOException {
        Path source = write(dir, PROGRAMA, PROGRAMA_11);

        assertEquals(
                ExitCode.SUCCESS.code(),
                CliTestSupport.run(FLAG_VERSION, V11, "validation", source.toString()).exitCode());
        assertEquals(
                ExitCode.SUCCESS.code(),
                CliTestSupport.run(FLAG_VERSION, V11, "formatting", source.toString()).exitCode());
        assertEquals(
                ExitCode.SUCCESS.code(),
                CliTestSupport.run(FLAG_VERSION, V11, "analyzing", source.toString()).exitCode());
    }

    // original es un alias de System.in para restaurarlo al final, no un recurso propio
    @Test
    @SuppressWarnings("PMD.CloseResource")
    void readInputTakesItsLineFromStandardInput(@TempDir Path dir) throws IOException {
        Path source =
                write(
                        dir,
                        "pide.ps",
                        "let nombre: string = readInput(\"nombre: \");\nprintln(nombre);\n");
        java.io.InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Joe\n".getBytes(StandardCharsets.UTF_8)));

            CliTestSupport.Result result =
                    CliTestSupport.run(FLAG_VERSION, V11, EJECUTAR, source.toString());

            assertTrue(result.stdout().contains("nombre: "), result.stdout());
            assertTrue(result.stdout().contains("Joe"), result.stdout());
        } finally {
            System.setIn(original);
        }
    }

    // --- la interfaz interactiva ---

    private record Output(String out, String err) {}

    private Output shell(Path dir, String script) {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);
        new InteractiveShell(in, out, err, dir).run();
        out.flush();
        err.flush();
        return new Output(
                outBuffer.toString(StandardCharsets.UTF_8),
                errBuffer.toString(StandardCharsets.UTF_8));
    }

    @Test
    void theShellStartsIn10AndCanSwitchTo11(@TempDir Path dir) throws IOException {
        write(dir, PROGRAMA, PROGRAMA_11);

        Output before = shell(dir, "execution programa.ps\nexit\n");
        Output after = shell(dir, "version 1.1\nexecution programa.ps\nexit\n");

        assertTrue(before.err().contains("Expected"), before.err());
        assertTrue(after.out().contains("hola"), after.out());
    }

    @Test
    void theShellRejectsAVersionThatDoesNotExist(@TempDir Path dir) {
        Output output = shell(dir, "version 9.9\nexit\n");

        assertTrue(output.err().contains("1.0, 1.1"), output.err());
    }

    @Test
    void readInputInsideTheShellReadsFromTheSameReaderAsTheCommands(@TempDir Path dir)
            throws IOException {
        // el caso que rompe si readInput abre un segundo lector sobre System.in: la
        // respuesta del usuario viene en la línea siguiente al comando
        write(dir, "pide.ps", "let nombre: string = readInput(\"nombre: \");\nprintln(nombre);\n");

        Output output = shell(dir, "version 1.1\nexecution pide.ps\nJoe\nexit\n");

        assertTrue(output.out().contains("nombre: "), output.out());
        assertTrue(output.out().contains("Joe"), output.out());
    }
}
