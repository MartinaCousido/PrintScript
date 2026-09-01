package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runner.ValidationRunner;

// Interfaz interactiva (REPL) que se lanza cuando la CLI se corre sin argumentos.
// Mantiene un directorio de trabajo propio para navegar el filesystem (pwd/ls/cd) y
// corre las operaciones sobre archivos relativos a ese directorio, reusando el mismo
// SourceFileProcessor que los subcomandos. Es el único módulo que toca el filesystem
// y stdin/stdout (docs/arquitectura.md, sección 5).
final class InteractiveShell {

    private final BufferedReader input;
    private final PrintStream out;
    private final PrintStream err;
    // cada alias apunta al handler de su comando; evita un switch gigante y hace de
    // única fuente de verdad de qué comandos existen
    private final Map<String, Consumer<String>> commands = new HashMap<>();
    private Path workingDir;
    private boolean running = true;

    InteractiveShell(InputStream in, PrintStream out, PrintStream err, Path startDir) {
        this.input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.err = err;
        this.workingDir = startDir.toAbsolutePath().normalize();
        registerCommands();
    }

    private void registerCommands() {
        register(argument -> printHelp(), "help", "?", "ayuda");
        register(argument -> out.println(workingDir), "pwd");
        register(this::listDirectory, "ls", "dir");
        register(this::changeDirectory, "cd");
        register(this::runExecution, "execution", "exec", "run");
        register(this::runValidation, "validation", "validate");
        register(
                argument -> reportUnsupported("Formatting", "formatter"),
                "formatting",
                "format",
                "fmt");
        register(
                argument -> reportUnsupported("Analyzing", "linter/semantic"),
                "analyzing",
                "analyze",
                "lint");
        register(argument -> running = false, "exit", "quit", "salir");
    }

    private void register(Consumer<String> handler, String... aliases) {
        for (String alias : aliases) {
            commands.put(alias, handler);
        }
    }

    // devuelve el código de salida del proceso; 0 al terminar el REPL normalmente
    int run() {
        printBanner();
        while (running) {
            out.print(prompt());
            out.flush();
            String line = readLine();
            if (line == null) { // EOF (Ctrl-D): salida limpia
                out.println();
                break;
            }
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                dispatch(trimmed);
            }
        }
        return ExitCode.SUCCESS.code();
    }

    private String readLine() {
        try {
            return input.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // separa el comando (primer token) del resto de la línea, que se toma tal cual
    // como argumento; así los paths con espacios funcionan sin comillas
    private void dispatch(String line) {
        int space = line.indexOf(' ');
        String command = (space < 0 ? line : line.substring(0, space)).toLowerCase(Locale.ROOT);
        String argument = space < 0 ? "" : line.substring(space + 1).strip();

        Consumer<String> handler = commands.get(command);
        if (handler == null) {
            err.println(
                    "Comando desconocido: '"
                            + command
                            + "'. Escribí 'help' para ver los comandos disponibles.");
            return;
        }
        handler.accept(argument);
    }

    // --- operaciones sobre archivos ---

    private void runExecution(String argument) {
        withSourceFile(
                argument,
                path ->
                        SourceFileProcessor.process(
                                path,
                                lines ->
                                        new ExecutionRunner()
                                                .run(lines, new ConsoleOutputPrinter(out)),
                                err));
    }

    private void runValidation(String argument) {
        withSourceFile(
                argument,
                path ->
                        SourceFileProcessor.process(
                                path, lines -> new ValidationRunner().validate(lines), err));
    }

    private void reportUnsupported(String operation, String missingModule) {
        err.println(
                operation
                        + " no está implementado todavía: el módulo "
                        + missingModule
                        + " no existe (ver docs/arquitectura.md).");
    }

    @FunctionalInterface
    private interface SourceAction {
        void run(Path resolved);
    }

    private void withSourceFile(String argument, SourceAction action) {
        if (argument.isEmpty()) {
            err.println("Falta el archivo. Uso: <operación> <archivo.ps>");
            return;
        }
        action.run(resolve(argument));
    }

    // --- navegación del filesystem ---

    private void listDirectory(String argument) {
        Path target = argument.isEmpty() ? workingDir : resolve(argument);
        if (!Files.isDirectory(target)) {
            err.println("No es un directorio: " + target);
            return;
        }
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    dirs.add(name + "/");
                } else {
                    files.add(name);
                }
            }
        } catch (IOException e) {
            err.println("No se pudo listar '" + target + "': " + e.getMessage());
            return;
        }
        dirs.sort(String::compareToIgnoreCase);
        files.sort(String::compareToIgnoreCase);
        if (dirs.isEmpty() && files.isEmpty()) {
            out.println("(directorio vacío)");
            return;
        }
        dirs.forEach(out::println);
        files.forEach(out::println);
    }

    private void changeDirectory(String argument) {
        if (argument.isEmpty()) {
            err.println("Falta el directorio. Uso: cd <directorio>");
            return;
        }
        Path target = resolve(argument);
        if (!Files.isDirectory(target)) {
            err.println("No es un directorio: " + target);
            return;
        }
        workingDir = target;
    }

    private Path resolve(String argument) {
        return workingDir.resolve(argument).normalize();
    }

    // --- presentación ---

    private String prompt() {
        return "printscript:" + workingDir.getFileName() + "> ";
    }

    private void printBanner() {
        out.println("PrintScript — interfaz interactiva. Escribí 'help' para ver los comandos.");
        out.println("Directorio actual: " + workingDir);
    }

    private void printHelp() {
        out.println("Comandos disponibles:");
        out.println("  Operaciones sobre un archivo .ps");
        out.println("    execution|exec|run <archivo>     Ejecuta el archivo e imprime su salida");
        out.println("    validation|validate <archivo>    Valida sintaxis/semántica sin ejecutar");
        out.println("    formatting|format|fmt <archivo>  (no implementado todavía)");
        out.println("    analyzing|analyze|lint <archivo> (no implementado todavía)");
        out.println("  Navegación del filesystem");
        out.println("    pwd                  Muestra el directorio actual");
        out.println("    ls|dir [directorio]  Lista el contenido (por defecto, el actual)");
        out.println("    cd <directorio>      Cambia de directorio (acepta .. y rutas absolutas)");
        out.println("  Sesión");
        out.println("    help|?               Muestra esta ayuda");
        out.println("    exit|quit            Sale de la interfaz");
    }
}
