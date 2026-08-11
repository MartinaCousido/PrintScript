package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class LexerVersionProviderTest {

    private static Token token(TokenType type, String value, int startColumn, int endColumn) {
        Range range = new Range(new Position(1, startColumn), new Position(1, endColumn));
        return new Token(type, value, range);
    }

    @Test
    void version10ProvidesALexerThatTokenizesACompleteStatement() {
        LexerProvider provider = LexerVersionProvider.providerFor("1.0");
        Lexer lexer = provider.provide(List.of("let name: string = \"Joe\";").iterator());

        List<Token> tokens = new ArrayList<>();
        while (lexer.hasNext()) {
            tokens.add(lexer.getNext());
        }

        assertEquals(
            List.of(
                token(TokenType.KEYWORD, "let", 1, 3),
                token(TokenType.IDENTIFIER, "name", 5, 8),
                token(TokenType.PUNCTUATION, ":", 9, 9),
                token(TokenType.KEYWORD, "string", 11, 16),
                token(TokenType.OPERATOR, "=", 18, 18),
                token(TokenType.STRING, "Joe", 20, 24),
                token(TokenType.PUNCTUATION, ";", 25, 25)),
            tokens);
    }

    @Test
    void anUnknownVersionFailsWithAClearMessage() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> LexerVersionProvider.providerFor("9.9"));

        assertTrue(error.getMessage().contains("9.9"), error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}
