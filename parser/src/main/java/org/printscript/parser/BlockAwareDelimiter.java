package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

public final class BlockAwareDelimiter implements StatementDelimiter {

    @Override
    public boolean endsStatement(
            List<Token> accumulated, Token current, Supplier<Optional<Token>> next) {
        int depth = depthOf(accumulated);
        if (Tokens.isPunctuation(current, ";")) {
            return depth == 0;
        }
        if (!Tokens.isPunctuation(current, "}")) {
            return false;
        }
        if (depth == 0) {
            return true;
        }
        return depth != 1 ? false : !isElse(next.get());
    }

    @Override
    public String unterminatedMessage(List<Token> accumulated) {
        return depthOf(accumulated) > 0
                ? "Expected '}' before the end of the stream"
                : "Expected ';' before the end of the stream";
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
