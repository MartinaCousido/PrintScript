package org.printscript.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class InteractiveShellTest {

    private record Output(int exitCode, String out, String err) {}

    // corre el shell alimentándole el script por stdin, con workingDir en dir
    private Output run(Path dir, String script) {
        ByteArrayInputStream in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);
        int exitCode = new InteractiveShell(in, out, err, dir).run();
        out.flush();
        err.flush();
        return new Output(
                exitCode,
                outBuffer.toString(StandardCharsets.UTF_8),
                errBuffer.toString(StandardCharsets.UTF_8));
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    @Test
    void bannerHelpAndPwdOnStartup(@TempDir Path dir) {
        Output result = run(dir, "help\npwd\nexit\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("interfaz interactiva"), "muestra el banner");
        assertTrue(result.out().contains("Comandos disponibles"), "help lista comandos");
        assertTrue(result.out().contains(dir.toAbsolutePath().normalize().toString()));
    }

    @Test
    void executesValidFilePrintingItsOutput(@TempDir Path dir) throws IOException {
        writeFile(dir.resolve("hello.ps"), "let name: string = \"Joe\";\nprintln(name);\n");

        Output result = run(dir, "exec hello.ps\nexit\n");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("Joe"), "imprime la salida del programa");
    }

    @Test
    void validationReportsSyntaxErrors(@TempDir Path dir) throws IOException {
        writeFile(dir.resolve("roto.ps"), "let x number = 1;\n");

        Output result = run(dir, "validate roto.ps\nexit\n");

        assertTrue(result.err().contains("error:"), "reporta el error de sintaxis");
        assertFalse(result.out().contains("despues"), "validation no ejecuta nada");
    }

    @Test
    void navigatesFilesystemWithLsCdPwd(@TempDir Path dir) throws IOException {
        Files.createDirectory(dir.resolve("examples"));
        writeFile(dir.resolve("a.ps"), "println(1);\n");

        Output result = run(dir, "ls\ncd examples\npwd\nexit\n");

        assertTrue(result.out().contains("examples/"), "lista el subdirectorio con /");
        assertTrue(result.out().contains("a.ps"), "lista el archivo");
        assertTrue(result.out().contains(dir.resolve("examples").toString()), "pwd refleja el cd");
    }

    @Test
    void reportsEmptyDirectory(@TempDir Path dir) {
        Output result = run(dir, "ls\nexit\n");

        assertTrue(result.out().contains("(directorio vacío)"));
    }

    @Test
    void formattingReportsNotImplemented(@TempDir Path dir) throws IOException {
        writeFile(dir.resolve("x.ps"), "println(1);\n");

        Output result = run(dir, "format x.ps\nexit\n");

        assertTrue(result.err().contains("Formatting no está implementado"));
    }

    @Test
    void analyzesWithConfigReportingViolations(@TempDir Path dir) throws IOException {
        writeFile(dir.resolve("snake.ps"), "let last_name: string = \"Doe\";\n");
        writeFile(
                dir.resolve("reglas.json"),
                "{ \"identifier_format\": \"camel case\","
                        + " \"mandatory-variable-or-literal-in-println\": true }");

        Output result = run(dir, "analyze snake.ps reglas.json\nanalyze\nexit\n");

        assertTrue(result.err().contains("lint:"), "reporta la violación de camelCase");
        assertTrue(result.err().contains("last_name"), result.err());
        assertTrue(result.err().contains("Falta el archivo"), "analyze sin archivo avisa");
    }

    @Test
    void reportsUsageErrors(@TempDir Path dir) {
        Output result =
                run(dir, "unknown\ncd nope\nexec\nexec falta.ps\nls archivo-inexistente\nexit\n");

        assertTrue(result.err().contains("Comando desconocido"));
        assertTrue(result.err().contains("No es un directorio"));
        assertTrue(result.err().contains("Falta el archivo"));
        assertTrue(result.err().contains("no existe o no es un archivo regular"));
    }

    @Test
    void endsCleanlyOnEndOfInput(@TempDir Path dir) {
        // sin comando exit: el REPL termina al agotarse el stdin (Ctrl-D)
        Output result = run(dir, "pwd\n");

        assertEquals(0, result.exitCode());
    }
}
