package org.printscript.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Las comillas agrupan rutas con espacios. Las barras de Windows se conservan.
final class ShellArguments {

    private static final int SINGLE_PATH_ARGUMENTS = 1;

    private static final Pattern ARGUMENT =
            Pattern.compile("\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"']+))(?=\\s|$)");

    private ShellArguments() {}

    static List<String> split(String input) {
        List<String> result = new ArrayList<>();
        Matcher matcher = ARGUMENT.matcher(input);
        int position = 0;
        while (position < input.length()) {
            matcher.region(position, input.length());
            if (!matcher.lookingAt()) {
                throw new IllegalArgumentException(
                        "Argumentos inválidos: usá comillas completas para las rutas con espacios.");
            }
            result.add(value(matcher));
            position = matcher.end();
        }
        return result;
    }

    private static String value(Matcher matcher) {
        if (matcher.group(1) != null) {
            return matcher.group(1);
        }
        return matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
    }

    static String path(String input) {
        if (!input.startsWith("\"") && !input.startsWith("'")) {
            return input;
        }
        List<String> arguments = split(input);
        if (arguments.size() != SINGLE_PATH_ARGUMENTS) {
            throw new IllegalArgumentException("Se esperaba una sola ruta.");
        }
        return arguments.getFirst();
    }
}
