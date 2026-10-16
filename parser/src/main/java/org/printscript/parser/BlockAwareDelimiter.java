package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

public final class BlockAwareDelimiter implements StatementDelimiter {

    @Override
    public boolean endsStatement(
            List<Token> accumulated, Token current, Supplier<Optional<Token>> next) {
        return endsAtDepth(depthOf(accumulated), current, next);
    }

    private static boolean endsAtDepth(int depth, Token current, Supplier<Optional<Token>> next) {
        if (Tokens.isPunctuation(current, ";")) {
            return depth == 0;
        }
        if (!Tokens.isPunctuation(current, "}")) {
            return false;
        }
        if (depth == 0) {
            return true;
        }
        // solo acá se consulta el token siguiente, que cuesta una línea más de fuente:
        // un 'else' continúa la misma sentencia, y con eso 'else if' sale gratis
        boolean closesOutermostBlock = depth == 1;
        return closesOutermostBlock && !isElse(next.get());
    }

    @Override
    public String unterminatedMessage(List<Token> accumulated) {
        return missingTerminator(depthOf(accumulated));
    }

    private static String missingTerminator(int depth) {
        return depth > 0
                ? "Expected '}' before the end of the stream"
                : "Expected ';' before the end of the stream";
    }

    @Override
    public StatementDelimiter forStatement() {
        return new Scanner();
    }

    /** Counts braces once as tokens arrive, without rescanning the accumulated list. */
    private static final class Scanner implements StatementDelimiter {
        private int depth;

        @Override
        public boolean endsStatement(
                List<Token> accumulated, Token current, Supplier<Optional<Token>> next) {
            boolean ends = endsAtDepth(depth, current, next);
            if (Tokens.isPunctuation(current, "{")) {
                depth++;
            } else if (Tokens.isPunctuation(current, "}")) {
                depth--;
            }
            return ends;
        }

        @Override
        public String unterminatedMessage(List<Token> accumulated) {
            return missingTerminator(depth);
        }
    }

    private static boolean isElse(Optional<Token> token) {
        return token.filter(candidate -> Tokens.isKeyword(candidate, "else")).isPresent();
    }

    private static int depthOf(List<Token> accumulated) {
        int depth = 0;
        for (Token token : accumulated) {
            if (Tokens.isPunctuation(token, "{")) {
                depth = depth + 1;
            } else if (Tokens.isPunctuation(token, "}")) {
                depth = depth - 1;
            }
        }
        return depth;
    }
}
