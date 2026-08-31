package org.printscript.cli;

import picocli.CommandLine;

public final class Cli {

    private Cli() {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
