package org.printscript.cli;

import java.util.ArrayList;
import java.util.List;

/** REPL arguments: quoted paths, escaped quotes/backslashes, and whitespace separators. */
final class ShellArguments {
    private static final int SINGLE_PATH_COUNT = 1;

    private ShellArguments() {}

    static String singlePath(String argument) {
        boolean quoted = argument.indexOf('"') >= 0 || argument.indexOf('\'') >= 0;
        if (!quoted) {
            return argument;
        }
        List<String> parts = tokenize(argument);
        if (parts.size() != SINGLE_PATH_COUNT) {
            throw new IllegalArgumentException("Se esperaba una sola ruta");
        }
        return parts.getFirst();
    }

    static List<String> tokenize(String argument) {
        return new Tokenizer(argument).parse();
    }

    private static final class Tokenizer {
        private final String argument;
        private final List<String> parts = new ArrayList<>();
        private char quote;
        private boolean started;
        private int index;

        private Tokenizer(String argument) {
            this.argument = argument;
        }

        private List<String> parse() {
            StringBuilder current = new StringBuilder();
            while (index < argument.length()) {
                appendNext(current);
                index++;
            }
            if (quote != 0) {
                throw new IllegalArgumentException("Comillas sin cerrar en los argumentos");
            }
            finishPart(current);
            return List.copyOf(parts);
        }

        private void appendNext(StringBuilder current) {
            char character = argument.charAt(index);
            if (isEscape(character)) {
                index++;
                current.append(argument.charAt(index));
                started = true;
            } else if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    current.append(character);
                }
            } else if (character == '"' || character == '\'') {
                quote = character;
                started = true;
            } else if (Character.isWhitespace(character)) {
                finishPart(current);
            } else {
                current.append(character);
                started = true;
            }
        }

        private boolean isEscape(char character) {
            if (character != '\\' || index + 1 >= argument.length()) {
                return false;
            }
            char escaped = argument.charAt(index + 1);
            return escaped == '\\' || escaped == '"' || escaped == '\'';
        }

        private void finishPart(StringBuilder current) {
            if (started) {
                parts.add(current.toString());
                current.setLength(0);
                started = false;
            }
        }
    }
}
