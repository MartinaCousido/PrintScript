package org.printscript.cli;

import java.nio.file.Path;
import picocli.CommandLine;

public final class Cli {

    private Cli() {}

    public static void main(String[] args) {
        int exitCode = args.length == 0 ? launchInteractiveShell() : runCommand(args);
        System.exit(exitCode);
    }

    // sin argumentos: se abre la interfaz interactiva (REPL) en vez de fallar por falta de
    // operación
    private static int launchInteractiveShell() {
        return new InteractiveShell(System.in, System.out, System.err, Path.of("")).run();
    }

    private static int runCommand(String... args) {
        return new CommandLine(new RootCommand()).execute(args);
    }
}
