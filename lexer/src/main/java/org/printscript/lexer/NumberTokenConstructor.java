package org.printscript.lexer;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;
//reconoce números, enteros y con coma decimal
public final class NumberTokenConstructor implements TokenConstructor {

    @Override
    public Optional<Token> tryBuild(String rest, Position start) {
        int length = digitsFrom(rest, 0);
        if (length == 0) {
            return Optional.empty();
        }
        if (hasDecimalPartAt(rest, length)) {
            length = digitsFrom(rest, length + 1);
        }
        String lexeme = rest.substring(0, length);
        Position end = new Position(start.line(), start.column() + length - 1);
        return Optional.of(new Token(TokenType.NUMBER, lexeme, new Range(start, end)));
    }

    private static int digitsFrom(String rest, int index) {
        int current = index;
        while (current < rest.length() && Character.isDigit(rest.charAt(current))) {
            current = current + 1;
        }
        return current;
    }

    private static boolean hasDecimalPartAt(String rest, int index) {
        return index < rest.length()
            && rest.charAt(index) == '.'
            && index + 1 < rest.length()
            && Character.isDigit(rest.charAt(index + 1));
    }
}
