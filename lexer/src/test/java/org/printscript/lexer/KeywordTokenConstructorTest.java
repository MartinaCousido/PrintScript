package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.lexer.constructors.KeywordTokenConstructor;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class KeywordTokenConstructorTest {

    private static Token buildAt(Map<String, TokenType> keywords, String rest, Position start) {
        Optional<Token> token = new KeywordTokenConstructor(keywords).tryBuild(rest, start);
        return token.orElseThrow();
    }

    @Test
    void aWordOfTheTableIsClassifiedWithItsMappedType() {
        Token token = buildAt(Map.of("let", TokenType.KEYWORD), "let x", new Position(1, 1));

        assertEquals(TokenType.KEYWORD, token.type());
        assertEquals("let", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 3)), token.range());
    }

    @Test
    void aWordThatMerelyStartsWithAKeywordIsAnIdentifier() {
        Token token = buildAt(Map.of("let", TokenType.KEYWORD), "letra", new Position(1, 1));

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("letra", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 5)), token.range());
    }

    @Test
    void identifiersAcceptDigitsAndUnderscores() {
        Token token = buildAt(Map.of("let", TokenType.KEYWORD), "_my_var2 = 1", new Position(1, 1));

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("_my_var2", token.value());
        assertEquals(new Range(new Position(1, 1), new Position(1, 8)), token.range());
    }

    @Test
    void theTableArrivesByParameterAndChangesTheClassification() {
        Position start = new Position(1, 1);

        assertEquals(TokenType.KEYWORD, buildAt(Map.of("println", TokenType.KEYWORD), "println", start).type());
        assertEquals(TokenType.IDENTIFIER, buildAt(Map.of("let", TokenType.KEYWORD), "println", start).type());
    }

    @Test
    void aWordMappedToBooleanIsClassifiedAsBooleanWithoutANewConstructorClass() {
        Token token = buildAt(Map.of("true", TokenType.BOOLEAN), "true", new Position(1, 1));

        assertEquals(TokenType.BOOLEAN, token.type());
        assertEquals("true", token.value());
    }
}
