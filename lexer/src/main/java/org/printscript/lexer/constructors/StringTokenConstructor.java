package org.printscript.lexer.constructors;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// reconoce textos entre comillas, simples o dobles
public final class StringTokenConstructor extends TextTokenConstructor {

    @Override
    public Optional<Token> tryBuild(CharSequence rest, Position start) {
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        char quote = rest.charAt(0);
        if (quote != '\'' && quote != '"') {
            return Optional.empty();
        }
        int closing = closingQuote(rest, quote);
        if (closing < 0) {
            return Optional.of(
                    new Token(TokenType.UNKNOWN, rest.toString(), rangeOf(start, rest.length())));
        }
        String content = rest.subSequence(1, closing).toString();
        return Optional.of(new Token(TokenType.STRING, content, rangeOf(start, closing + 1)));
    }

    private static int closingQuote(CharSequence text, char quote) {
        for (int index = 1; index < text.length(); index++) {
            if (text.charAt(index) == quote) {
                return index;
            }
        }
        return -1;
    }

    private static Range rangeOf(Position start, int length) {
        return new Range(start, new Position(start.line(), start.column() + length - 1));
    }
}
