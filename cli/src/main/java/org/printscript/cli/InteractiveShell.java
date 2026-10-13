package org.printscript.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.printscript.runner.ValidationRunner;

// Interfaz interactiva (REPL) que se lanza cuando la CLI se corre sin argumentos.
// Mantiene un directorio de trabajo propio para navegar el filesystem (pwd/ls/cd) y
// corre las operaciones sobre archivos relativos a ese directorio, reusando el mismo
// SourceFileProcessor que los subcomandos. Es el único módulo que toca el filesystem
// y stdin/stdout (docs/arquitectura.md, sección 5).
final class InteractiveShell {

    private static final int SOURCE_AND_CONFIG_PARTS = 2;

    private final BufferedReader input;
    private final PrintStream out;
    private final PrintStream err;
    private final DirectoryNavigator navigator;
    // cada alias apunta al handler de su comando; evita un switch gigante y hace de
    // única fuente de verdad de qué comandos existen
    private final Map<String, Consumer<String>> commands = new HashMap<>();
    private boolean running = true;
    // la versión de la sesión: la fijan con el comando 'version'
    private LanguageVersion version = LanguageVersion.V1_0;

    InteractiveShell(InputStream in, PrintStream out, PrintStream err, Path startDir) {
        this.input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.err = err;
        this.navigator = new DirectoryNavigator(startDir, out, err);
        registerCommands();
    }

    private void registerCommands() {
        register(argument -> printHelp(), "help", "?", "ayuda");
        register(argument -> out.println(navigator.current()), "pwd");
        register(navigator::list, "ls", "dir");
        register(navigator::change, "cd");
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
        String[] parts = line.split("\\s+", SOURCE_AND_CONFIG_PARTS);
        String command = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].strip() : "";

        Consumer<String> handler = commands.get(command);
        if (handler == null) {
            err.println(
                    "Comando desconocido: '"
                            + command
                            + "'. Escribí 'help' para ver los comandos disponibles.");
            return;
        }
        try {
            handler.accept(argument);
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
        }
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
        action.run(navigator.resolve(argument));
    }

    // <archivo> [config]: el segundo token opcional es el archivo de reglas
    private void withSourceAndConfig(String argument, String uso, ConfiguredOperation operation) {
        if (argument.isEmpty()) {
            err.println("Falta el archivo. Uso: " + uso);
            return;
        }
        List<String> parts = ShellArguments.split(argument);
        if (parts.size() > SOURCE_AND_CONFIG_PARTS) {
            err.println(
                    "Demasiados argumentos. Uso: "
                            + uso
                            + ". Usá comillas para las rutas con espacios.");
            return;
        }
        Path source = navigator.resolve(parts.getFirst());
        Path config = parts.size() > 1 ? navigator.resolve(parts.get(1)) : null;
        operation.run(source, config);
    }

    // --- presentación ---

    private String prompt() {
        return "printscript:" + navigator.current().getFileName() + "> ";
    }

    private void printBanner() {
        out.println("PrintScript — interfaz interactiva. Escribí 'help' para ver los comandos.");
        out.println("Directorio actual: " + navigator.current());
    }

    private void printHelp() {
        out.println("Comandos disponibles  (<x> es obligatorio, [x] es opcional):");
        out.println();
        printFileCommands();
        printConfigNote();
        out.println("    Rutas con espacios: format \"mi programa.ps\" \"mis reglas.json\"");
        out.println();
        printNavigationCommands();
        printSessionCommands();
    }

    private void printFileCommands() {
        out.println("  Sobre un archivo .ps");
        helpLine("run|exec <archivo>", "Ejecuta el archivo e imprime la salida por pantalla");
        helpLine(
                "save <archivo>",
                "Como run, y además guarda la salida en <fuente>.out.txt junto al .ps");
        helpLine(
                "validate <archivo>", "Valida la sintaxis y la semántica sin ejecutar el programa");
        helpLine("format <archivo> [config]", "Formatea el archivo e imprime el resultado");
        helpLine("analyze <archivo> [config]", "Lintea el archivo y reporta las violaciones");
        out.println();
    }

    private void printConfigNote() {
        out.println("    [config] es un archivo de reglas .json/.yaml/.yml (opcional):");
        out.println("      · format sin [config]  → no aplica reglas de formato; muestra un aviso");
        out.println("      · analyze sin [config] → solo revisa sintaxis; muestra un aviso");
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
        helpLine("version [1.0|1.1]", "Muestra o cambia la versión del lenguaje (inicial: 1.0)");
        helpLine("help|?", "Muestra esta ayuda");
        helpLine("exit|quit", "Sale de la interfaz (también con Ctrl-D)");
    }

    private void helpLine(String command, String description) {
        out.println(String.format(Locale.ROOT, "    %-28s%s", command, description));
    }
}
