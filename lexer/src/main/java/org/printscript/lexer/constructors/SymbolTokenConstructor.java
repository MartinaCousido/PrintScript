package org.printscript.lexer.constructors;

import java.util.Map;
import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// reconoce los simbolos: + - * / = ; : ( )
public final class SymbolTokenConstructor extends TextTokenConstructor {

    private final Map<String, TokenType> symbols;

    public SymbolTokenConstructor(Map<String, TokenType> symbols) {
        this.symbols = Map.copyOf(symbols);
    }

    @Override
    public Optional<Token> tryBuild(CharSequence rest, Position start) {
        String longest = null;
        for (String lexeme : symbols.keySet()) {
            if (startsWith(rest, lexeme) && isLongerThan(lexeme, longest)) {
                longest = lexeme;
            }
        }
        if (longest == null) {
            return Optional.empty();
        }
        Position end = new Position(start.line(), start.column() + longest.length() - 1);
        return Optional.of(new Token(symbols.get(longest), longest, new Range(start, end)));
    }

    private static boolean startsWith(CharSequence text, String prefix) {
        if (prefix.length() > text.length()) {
            return false;
        }
        for (int index = 0; index < prefix.length(); index++) {
            if (text.charAt(index) != prefix.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLongerThan(String lexeme, String current) {
        return current == null || lexeme.length() > current.length();
    }
}
