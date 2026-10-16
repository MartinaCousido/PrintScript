package org.printscript.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Mantiene el directorio de la sesión y presenta sus entradas de forma estable.
final class DirectoryNavigator {

    private final PrintStream out;
    private final PrintStream err;
    private Path workingDir;

    DirectoryNavigator(Path startDir, PrintStream out, PrintStream err) {
        this.workingDir = startDir.toAbsolutePath().normalize();
        this.out = out;
        this.err = err;
    }

    Path current() {
        return workingDir;
    }

    Path resolve(String argument) {
        return workingDir.resolve(argument).normalize();
    }

    void list(String argument) {
        Path target = argument.isEmpty() ? workingDir : resolve(argument);
        if (!Files.isDirectory(target)) {
            err.println("No es un directorio: " + target);
            return;
        }
        entries(target).ifPresent(this::printEntries);
    }

    void change(String argument) {
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

    private Optional<List<String>> entries(Path target) {
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
            for (Path entry : stream) {
                classify(entry, dirs, files);
            }
        } catch (IOException exception) {
            err.println("No se pudo listar '" + target + "': " + exception.getMessage());
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
}
