package org.printscript.lexer;

import java.util.Optional;
import java.util.Set;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

public final class KeywordTokenConstructor implements TokenConstructor {

    private final Set<String> keywords;

    public KeywordTokenConstructor(Set<String> keywords) {
        this.keywords = keywords;
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
        TokenType type = keywords.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
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
