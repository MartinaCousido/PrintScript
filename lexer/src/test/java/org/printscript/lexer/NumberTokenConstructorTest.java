package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.lexer.constructors.NumberTokenConstructor;
import org.printscript.lexer.constructors.TokenConstructor;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class NumberTokenConstructorTest {

    private final TokenConstructor constructor = new NumberTokenConstructor();

    private Token buildAt(String rest, Position start) {
        Optional<Token> token = constructor.tryBuild(rest, start);
        return token.orElseThrow();
    }

    @Test
    void integersAreRecognized() {
        Token token = buildAt("42", new Position(1, 1));

        assertEquals(TokenType.NUMBER, token.type());
        assertEquals("42", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 2)), token.range());
    }

    @Test
    void decimalsAreRecognized() {
        Token token = buildAt("3.14", new Position(1, 1));

        assertEquals(TokenType.NUMBER, token.type());
        assertEquals("3.14", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 4)), token.range());
    }

    @Test
    void aDotNotFollowedByADigitIsNotPartOfTheNumber() {
        Token token = buildAt("12.", new Position(1, 1));

        assertEquals("12", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 2)), token.range());
    }

    @Test
    void theRangeReflectsANumberThatDoesNotStartAtColumnOne() {
        Token token = buildAt("250;", new Position(2, 9));

        assertEquals("250", token.value());
        assertEquals(new Range(new Position(2, 9), new Position(2, 11)), token.range());
    }
}
