package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.lexer.constructors.KeywordTokenConstructor;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class KeywordTokenConstructorTest {

    private static Token buildAt(Set<String> keywords, String rest, Position start) {
        Optional<Token> token = new KeywordTokenConstructor(keywords).tryBuild(rest, start);
        return token.orElseThrow();
    }

    @Test
    void aWordOfTheKeywordSetIsAKeyword() {
        Token token = buildAt(Set.of("let"), "let x", new Position(1, 1));

        assertEquals(TokenType.KEYWORD, token.type());
        assertEquals("let", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 3)), token.range());
    }

    @Test
    void aWordThatMerelyStartsWithAKeywordIsAnIdentifier() {
        Token token = buildAt(Set.of("let"), "letra", new Position(1, 1));

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("letra", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 5)), token.range());
    }

    @Test
    void identifiersAcceptDigitsAndUnderscores() {
        Token token = buildAt(Set.of("let"), "_my_var2 = 1", new Position(1, 1));

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("_my_var2", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 8)), token.range());
    }

    @Test
    void theKeywordSetArrivesByParameterAndChangesTheClassification() {
        Position start = new Position(1, 1);

        assertEquals(TokenType.KEYWORD, buildAt(Set.of("println"), "println", start).type());
        assertEquals(TokenType.IDENTIFIER, buildAt(Set.of("let"), "println", start).type());
    }
}
