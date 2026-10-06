package org.printscript.lexer.constructors;

import java.util.Map;
import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// reconoce palabras y decide su categoría según la tabla que le pasan
public final class KeywordTokenConstructor implements TokenConstructor {

    private final Map<String, TokenType> keywords;

    public KeywordTokenConstructor(Map<String, TokenType> keywords) {
        this.keywords = Map.copyOf(keywords);
    }

    @Override
    public Optional<Token> tryBuild(String rest, Position start) {
        if (rest.isEmpty() || !startsWord(rest.charAt(0))) {
            return Optional.empty();
        }
        int length = 1;
        while (length < rest.length() && continuesWord(rest.charAt(length))) {
            length = length + 1;
        }
        String word = rest.substring(0, length);
        TokenType type = keywords.getOrDefault(word, TokenType.IDENTIFIER);
        Position end = new Position(start.line(), start.column() + length - 1);
        return Optional.of(new Token(type, word, new Range(start, end)));
    }

    private static boolean startsWord(char character) {
        return Character.isLetter(character) || character == '_';
    }

    private static boolean continuesWord(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }
}
