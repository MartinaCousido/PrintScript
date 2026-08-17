package org.printscript.lexer.constructors;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;
//reconoce textos entre comillas, simples o dobles
public final class StringTokenConstructor implements TokenConstructor {

    @Override
    public Optional<Token> tryBuild(String rest, Position start) {
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        char quote = rest.charAt(0);
        if (quote != '\'' && quote != '"') {
            return Optional.empty();
        }
        int closing = rest.indexOf(quote, 1);
        if (closing < 0) {
            return Optional.of(new Token(TokenType.UNKNOWN, rest, rangeOf(start, rest.length())));
        }
        String content = rest.substring(1, closing);
        return Optional.of(new Token(TokenType.STRING, content, rangeOf(start, closing + 1)));
    }

    private static Range rangeOf(Position start, int length) {
        return new Range(start, new Position(start.line(), start.column() + length - 1));
    }
}
