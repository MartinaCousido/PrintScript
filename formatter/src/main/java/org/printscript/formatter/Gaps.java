package org.printscript.formatter;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// Decide qué va entre dos tokens.
final class Gaps {

    private static final Set<String> OPERATIONS = Set.of("+", "-", "*", "/");

    private final FormatterConfiguration configuration;

    private int depth;
    private boolean insidePrintln;

    Gaps(FormatterConfiguration configuration) {
        this.configuration = configuration;
    }

    String between(Token previous, Token current) {
        return forced(previous, current).orElseGet(() -> original(previous, current));
    }

    // el estado se actualiza después de decidir el espacio, porque la decisión mira la
    // sentencia que termina, no la que empieza
    void advancePast(Token previous, Token current) {
        if (previous != null && isPunctuation(previous, ";")) {
            insidePrintln = false;
        }
        if (isKeyword(current, "println")) {
            insidePrintln = true;
        }
        if (isPunctuation(current, "{")) {
            depth = depth + 1;
        } else if (isPunctuation(current, "}")) {
            depth = depth - 1;
        }
    }

    // las reglas se consultan en orden y gana la primera que opine
    private Optional<String> forced(Token previous, Token current) {
        return Stream.<Supplier<Optional<String>>>of(
                        () -> afterPrintln(previous, current),
                        () -> afterStatement(previous, current),
                        () -> bracePlacement(current),
                        () -> aroundEquals(previous, current),
                        () -> beforeColon(current),
                        () -> afterColon(previous),
                        () -> aroundOperations(previous, current),
                        () -> singleSpace(previous, current))
                .map(Supplier::get)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> afterPrintln(Token previous, Token current) {
        if (!insidePrintln || !isPunctuation(previous, ";")) {
            return Optional.empty();
        }
        return configuration
                .lineBreaksAfterPrintln()
                .map(howMany -> lineBreaks(howMany + 1, current));
    }

    // solo si la sentencia siguiente arranca en la misma línea: si ya había un salto, la
    // regla está cumplida y no hay nada que imponer
    private Optional<String> afterStatement(Token previous, Token current) {
        boolean applies =
                configuration.lineBreakAfterStatement()
                        && isPunctuation(previous, ";")
                        && sameLine(previous, current);
        return applies ? Optional.of(lineBreaks(1, current)) : Optional.empty();
    }

    private Optional<String> bracePlacement(Token current) {
        if (!isPunctuation(current, "{")) {
            return Optional.empty();
        }
        return configuration
                .ifBraceOnSameLine()
                .map(onSameLine -> onSameLine ? " " : lineBreaks(1, current));
    }

    private Optional<String> aroundEquals(Token previous, Token current) {
        if (!isOperator(previous, "=") && !isOperator(current, "=")) {
            return Optional.empty();
        }
        return configuration.spacingAroundEquals().map(spaced -> spaced ? " " : "");
    }

    private Optional<String> beforeColon(Token current) {
        return configuration.spacingBeforeColon() && isPunctuation(current, ":")
                ? Optional.of(" ")
                : Optional.empty();
    }

    private Optional<String> afterColon(Token previous) {
        return configuration.spacingAfterColon() && isPunctuation(previous, ":")
                ? Optional.of(" ")
                : Optional.empty();
    }

    private Optional<String> aroundOperations(Token previous, Token current) {
        return configuration.spaceSurroundingOperations()
                        && (isOperation(previous) || isOperation(current))
                ? Optional.of(" ")
                : Optional.empty();
    }

    // todo separado por un espacio, salvo el ';', que se queda contra lo que cierra
    private Optional<String> singleSpace(Token previous, Token current) {
        if (!configuration.singleSpaceSeparation() || !sameLine(previous, current)) {
            return Optional.empty();
        }
        return Optional.of(isPunctuation(current, ";") ? "" : " ");
    }

    private String original(Token previous, Token current) {
        int breaks = current.range().start().line() - previous.range().end().line();
        if (breaks == 0) {
            return spaces(current.range().start().column() - previous.range().end().column() - 1);
        }
        // el salto ya estaba: la columna del token es su sangría real en el archivo
        return "\n".repeat(breaks) + indentBefore(current, current.range().start().column() - 1);
    }

    private String lineBreaks(int howMany, Token current) {
        return "\n".repeat(howMany) + indentBefore(current, 0);
    }

    // la sangría configurada manda sobre la que traía el archivo; el '}' cierra su nivel,
    // así que se alinea con la línea del if y no con el cuerpo
    private String indentBefore(Token current, int asItWas) {
        int level = isPunctuation(current, "}") ? depth - 1 : depth;
        return configuration
                .indentInsideIf()
                .map(size -> spaces(size * level))
                .orElseGet(() -> spaces(asItWas));
    }

    private static String spaces(int howMany) {
        return " ".repeat(Math.max(0, howMany));
    }

    private static boolean sameLine(Token previous, Token current) {
        return previous.range().end().line() == current.range().start().line();
    }

    private static boolean isOperation(Token token) {
        return token.type() == TokenType.OPERATOR && OPERATIONS.contains(token.value());
    }

    private static boolean isOperator(Token token, String text) {
        return token.type() == TokenType.OPERATOR && text.equals(token.value());
    }

    private static boolean isPunctuation(Token token, String text) {
        return token.type() == TokenType.PUNCTUATION && text.equals(token.value());
    }

    private static boolean isKeyword(Token token, String text) {
        return token.type() == TokenType.KEYWORD && text.equals(token.value());
    }
}
