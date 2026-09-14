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
import java.util.Optional;
import java.util.function.Consumer;
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
    // la versión de la sesión: la fijan con el comando 'version'
    private LanguageVersion version = LanguageVersion.V1_0;

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
        register(this::runSave, "save", "guardar");
        register(this::runValidation, "validation", "validate");
        register(this::runFormatting, "formatting", "format", "fmt");
        register(this::runAnalyzing, "analyzing", "analyze", "lint");
        register(this::runVersion, "version", "v");
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

    // version           muestra la versión de la sesión
    // version 1.1       la cambia; las cinco operaciones la respetan desde ahí
    private void runVersion(String argument) {
        if (argument.isEmpty()) {
            out.println("Versión del lenguaje: " + version.label());
            return;
        }
        LanguageVersion elegida = LanguageVersion.of(argument);
        if (elegida == null) {
            err.println(
                    "'"
                            + argument
                            + "' no es una versión soportada (valores válidos: "
                            + LanguageVersion.supported()
                            + ")");
            return;
        }
        version = elegida;
        out.println("Versión del lenguaje: " + version.label());
    }

    private void runExecution(String argument) {
        withSourceFile(
                argument,
                source -> ExecutionOutput.run(source, null, out, err, version.domain(), input));
    }

    // save <archivo>: ejecuta y guarda la salida en un .txt junto al fuente
    private void runSave(String argument) {
        withSourceFile(
                argument,
                source ->
                        ExecutionOutput.run(
                                source,
                                ExecutionOutput.defaultOutputFor(source),
                                out,
                                err,
                                version.domain(),
                                input));
    }

    private void runValidation(String argument) {
        withSourceFile(
                argument,
                path ->
                        SourceFileProcessor.process(
                                path,
                                lines -> new ValidationRunner().validate(lines, version.domain()),
                                err));
    }

    private void runAnalyzing(String argument) {
        withSourceAndConfig(
                argument,
                "analyze <archivo.ps> [config.json|yaml]",
                (source, config) -> LintAnalysis.run(source, config, err, version.domain()));
    }

    private void runFormatting(String argument) {
        withSourceAndConfig(
                argument,
                "format <archivo.ps> [config.json|yaml]",
                (source, config) ->
                        Formatting.run(source, config, null, out, err, version.domain()));
    }

    @FunctionalInterface
    private interface SourceAction {
        void run(Path resolved);
    }

    @FunctionalInterface
    private interface ConfiguredOperation {
        void run(Path source, Path config);
    }

    private void withSourceFile(String argument, SourceAction action) {
        if (argument.isEmpty()) {
            err.println("Falta el archivo. Uso: <operación> <archivo.ps>");
            return;
        }
        action.run(resolve(argument));
    }

    // <archivo> [config]: el segundo token opcional es el archivo de reglas
    private void withSourceAndConfig(String argument, String uso, ConfiguredOperation operation) {
        if (argument.isEmpty()) {
            err.println("Falta el archivo. Uso: " + uso);
            return;
        }
        String[] parts = argument.split("\\s+", 2);
        Path source = resolve(parts[0]);
        Path config = parts.length > 1 ? resolve(parts[1]) : null;
        operation.run(source, config);
    }

    // --- navegación del filesystem ---

    private void listDirectory(String argument) {
        Path target = argument.isEmpty() ? workingDir : resolve(argument);
        if (!Files.isDirectory(target)) {
            err.println("No es un directorio: " + target);
            return;
        }
        entries(target).ifPresent(this::printEntries);
    }

    // vacío cuando no se pudo leer el directorio: el error ya se reportó
    private Optional<List<String>> entries(Path target) {
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
            for (Path entry : stream) {
                classify(entry, dirs, files);
            }
        } catch (IOException e) {
            err.println("No se pudo listar '" + target + "': " + e.getMessage());
            return Optional.empty();
        }
        return Optional.of(sorted(dirs, files));
    }

    private static void classify(Path entry, List<String> dirs, List<String> files) {
        String name = entry.getFileName().toString();
        if (Files.isDirectory(entry)) {
            dirs.add(name + "/");
        } else {
            files.add(name);
        }
    }

    // primero los directorios y después los archivos, cada grupo en orden alfabético
    private static List<String> sorted(List<String> dirs, List<String> files) {
        dirs.sort(String::compareToIgnoreCase);
        files.sort(String::compareToIgnoreCase);
        List<String> all = new ArrayList<>(dirs);
        all.addAll(files);
        return all;
    }

    private void printEntries(List<String> entries) {
        if (entries.isEmpty()) {
            out.println("(directorio vacío)");
            return;
        }
        entries.forEach(out::println);
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
        out.println("Comandos disponibles  (<x> es obligatorio, [x] es opcional):");
        out.println();
        printFileCommands();
        printConfigNote();
        printNavigationCommands();
        printSessionCommands();
    }

    private void printFileCommands() {
        out.println("  Sobre un archivo .ps");
        helpLine("run|exec <archivo>", "Ejecuta el archivo e imprime la salida por pantalla");
        helpLine(
                "save <archivo>",
                "Como run, y además guarda la salida en <fuente>.out.txt junto al .ps");
        helpLine("validate <archivo>", "Valida la sintaxis sin ejecutar el programa");
        helpLine("format <archivo> [config]", "Formatea el archivo e imprime el resultado");
        helpLine("analyze <archivo> [config]", "Lintea el archivo y reporta las violaciones");
        out.println();
    }

    private void printConfigNote() {
        out.println("    [config] es un archivo de reglas .json/.yaml/.yml (opcional):");
        out.println("      · format sin [config]  → reglas por defecto (sin espaciado extra)");
        out.println("      · analyze sin [config] → ninguna regla activa (no reporta nada)");
        out.println("      · alias: exec=execution, format=fmt=formatting, analyze=lint=analyzing");
        out.println();
    }

    private void printNavigationCommands() {
        out.println("  Navegación del filesystem (rutas relativas al directorio actual, pwd)");
        helpLine("pwd", "Muestra el directorio actual");
        helpLine("ls|dir [directorio]", "Lista el contenido (por defecto, el actual)");
        helpLine("cd <directorio>", "Cambia de directorio (acepta .. y rutas absolutas)");
        out.println();
    }

    private void printSessionCommands() {
        out.println("  Sesión");
        helpLine("help|?", "Muestra esta ayuda");
        helpLine("exit|quit", "Sale de la interfaz (también con Ctrl-D)");
    }

    private void helpLine(String command, String description) {
        out.println(String.format(Locale.ROOT, "    %-28s%s", command, description));
    }
}
