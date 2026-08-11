package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class PrintlnParserTest {

    private final PrintlnParser parser = new PrintlnParser(new ExpressionBuilder());

    private static Range range(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static Token token(TokenType type, String value, int column) {
        return new Token(type, value, range(column, column + value.length() - 1));
    }

    private static Token string(String value, int column) {
        return new Token(TokenType.STRING, value, range(column, column + value.length() + 1));
    }

    private static TokenBuffer bufferOf(Token... tokens) {
        return new TokenBuffer(new ListTokenIterator(List.of(tokens)));
    }

    @Test
    void matchesOnlyStatementsThatStartWithPrintln() {
        assertTrue(parser.matches(bufferOf(token(TokenType.KEYWORD, "println", 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.KEYWORD, "let", 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.IDENTIFIER, "println", 1))));
        assertFalse(parser.matches(bufferOf()));
    }

    @Test
    void parsesPrintlnWithAStringLiteral() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            string("Hello", 9),
            token(TokenType.PUNCTUATION, ")", 16),
            token(TokenType.PUNCTUATION, ";", 17));

        Result<Statement> result = parser.parse(tokens);

        Statement expected = new PrintlnStatement(
            new StringLiteral("Hello", range(9, 15)),
            range(1, 17));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parsesPrintlnWithAnIdentifier() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            token(TokenType.IDENTIFIER, "name", 9),
            token(TokenType.PUNCTUATION, ")", 13),
            token(TokenType.PUNCTUATION, ";", 14));

        Result<Statement> result = parser.parse(tokens);

        Statement expected = new PrintlnStatement(
            new Identifier("name", range(9, 12)),
            range(1, 14));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parsesPrintlnWithACompoundExpression() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            token(TokenType.IDENTIFIER, "a", 9),
            token(TokenType.OPERATOR, "+", 11),
            token(TokenType.NUMBER, "2", 13),
            token(TokenType.OPERATOR, "*", 15),
            token(TokenType.NUMBER, "3", 17),
            token(TokenType.PUNCTUATION, ")", 18),
            token(TokenType.PUNCTUATION, ";", 19));

        Result<Statement> result = parser.parse(tokens);

        Statement expected = new PrintlnStatement(
            new BinaryExpression(
                new Identifier("a", range(9, 9)),
                Operator.PLUS,
                new BinaryExpression(
                    new NumberLiteral(2.0, range(13, 13)),
                    Operator.MULTIPLY,
                    new NumberLiteral(3.0, range(17, 17)),
                    range(13, 17)),
                range(9, 17)),
            range(1, 19));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void missingOpenParenthesisFailsAtTheOffendingToken() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            string("x", 9),
            token(TokenType.PUNCTUATION, ";", 12));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(9, 11), failure.range());
    }

    @Test
    void missingCloseParenthesisFailsAtTheOffendingToken() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            token(TokenType.IDENTIFIER, "x", 9),
            token(TokenType.PUNCTUATION, ";", 10));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(10, 10), failure.range());
    }

    @Test
    void missingSemicolonFailsAfterTheCloseParenthesis() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            token(TokenType.IDENTIFIER, "x", 9),
            token(TokenType.PUNCTUATION, ")", 10));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(10, 10), failure.range());
    }

    @Test
    void emptyArgumentFailsAtTheCloseParenthesis() {
        TokenBuffer tokens = bufferOf(
            token(TokenType.KEYWORD, "println", 1),
            token(TokenType.PUNCTUATION, "(", 8),
            token(TokenType.PUNCTUATION, ")", 9),
            token(TokenType.PUNCTUATION, ";", 10));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(9, 9), failure.range());
    }
}
