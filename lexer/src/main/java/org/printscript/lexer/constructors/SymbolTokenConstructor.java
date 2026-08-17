package org.printscript.lexer.constructors;

import java.util.Map;
import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;
//reconoce los simbolos: + - * / = ; : ( )
public final class SymbolTokenConstructor implements TokenConstructor {

    private final Map<String, TokenType> symbols;

    public SymbolTokenConstructor(Map<String, TokenType> symbols) {
        this.symbols = symbols;
    }

    @Override
    public Optional<Token> tryBuild(String rest, Position start) {
        String longest = null;
        for (String lexeme : symbols.keySet()) {
            if (rest.startsWith(lexeme) && isLongerThan(lexeme, longest)) {
                longest = lexeme;
            }
        }
        if (longest == null) {
            return Optional.empty();
        }
        Position end = new Position(start.line(), start.column() + longest.length() - 1);
        return Optional.of(new Token(symbols.get(longest), longest, new Range(start, end)));
    }

    private static boolean isLongerThan(String lexeme, String current) {
        return current == null || lexeme.length() > current.length();
    }
}
