package org.printscript.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class LexerSelectionTest {

    private record FixedLexeme(String lexeme, TokenType type) implements TokenConstructor {

        @Override
        public Optional<Token> tryBuild(String rest, Position start) {
            if (!rest.startsWith(lexeme)) {
                return Optional.empty();
            }
            Position end = new Position(start.line(), start.column() + lexeme.length() - 1);
            return Optional.of(new Token(type, lexeme, new Range(start, end)));
        }
    }

    private static Lexer lexerOf(String line, List<TokenConstructor> constructors) {
        return new Lexer(List.of(line).iterator(), constructors);
    }

    @Test
    void theLongestMatchWins() {
        Lexer lexer = lexerOf(
            "abc",
            List.of(
                new FixedLexeme("ab", TokenType.OPERATOR),
                new FixedLexeme("abc", TokenType.IDENTIFIER)));

        Token token = lexer.getNext();

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("abc", token.value());
        assertFalse(lexer.hasNext());
    }

    @Test
    void aTieOfLengthIsResolvedByTheOrderOfTheList() {
        Lexer lexer = lexerOf(
            "ab",
            List.of(
                new FixedLexeme("ab", TokenType.OPERATOR),
                new FixedLexeme("ab", TokenType.PUNCTUATION)));

        assertEquals(TokenType.OPERATOR, lexer.getNext().type());
    }

    @Test
    void withoutMatchAnUnknownIsEmittedAndTheIterationContinues() {
        Lexer lexer = lexerOf("#b", List.of(new FixedLexeme("b", TokenType.IDENTIFIER)));

        Token unknown = lexer.getNext();
        assertEquals(TokenType.UNKNOWN, unknown.type());
        assertEquals("#", unknown.value());

        Token recognized = lexer.getNext();
        assertEquals(TokenType.IDENTIFIER, recognized.type());
        assertEquals("b", recognized.value());
    }
}
