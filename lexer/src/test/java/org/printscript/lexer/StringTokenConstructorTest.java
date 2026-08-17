package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.lexer.constructors.StringTokenConstructor;
import org.printscript.lexer.constructors.TokenConstructor;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class StringTokenConstructorTest {

    private final TokenConstructor constructor = new StringTokenConstructor();

    private Token buildAt(String rest, Position start) {
        Optional<Token> token = constructor.tryBuild(rest, start);
        return token.orElseThrow();
    }

    @Test
    void stringsWithDoubleQuotesAreRecognized() {
        Token token = buildAt("\"Joe\"", new Position(1, 1));

        assertEquals(TokenType.STRING, token.type());
        assertEquals("Joe", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 5)), token.range());
    }

    @Test
    void stringsWithSingleQuotesAreRecognized() {
        Token token = buildAt("'Joe'", new Position(1, 1));

        assertEquals(TokenType.STRING, token.type());
        assertEquals("Joe", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 5)), token.range());
    }

    @Test
    void theOtherQuoteIsPartOfTheContent() {
        Token token = buildAt("\"it's\"", new Position(1, 1));

        assertEquals("it's", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 6)), token.range());
    }

    @Test
    void theEmptyStringIsRecognized() {
        Token token = buildAt("''", new Position(1, 1));

        assertEquals(TokenType.STRING, token.type());
        assertEquals("", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 2)), token.range());
    }

    @Test
    void anUnclosedStringProducesUnknownUpToTheEndOfTheLine() {
        Token token = buildAt("\"abc", new Position(1, 3));

        assertEquals(TokenType.UNKNOWN, token.type());
        assertEquals(new Range(new Position(1, 3), new Position(1, 6)), token.range());
    }
}
