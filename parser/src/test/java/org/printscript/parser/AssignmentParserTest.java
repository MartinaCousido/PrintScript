package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class AssignmentParserTest {

    private final AssignmentParser parser = new AssignmentParser(new ExpressionBuilder());

    private static Range range(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static Token token(TokenType type, String value, int column) {
        return new Token(type, value, range(column, column + value.length() - 1));
    }

    private static TokenBuffer bufferOf(Token... tokens) {
        return new TokenBuffer(new ListTokenIterator(List.of(tokens)));
    }

    @Test
    void matchesAnIdentifierFollowedByEquals() {
        assertTrue(
                parser.matches(
                        bufferOf(
                                token(TokenType.IDENTIFIER, "x", 1),
                                token(TokenType.OPERATOR, "=", 3))));
    }

    @Test
    void matchesDistinguishesAssignmentFromDeclarationAndPrintln() {
        assertFalse(parser.matches(bufferOf(token(TokenType.KEYWORD, "let", 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.KEYWORD, "println", 1))));
        assertFalse(parser.matches(bufferOf(token(TokenType.IDENTIFIER, "x", 1))));
        assertFalse(
                parser.matches(
                        bufferOf(
                                token(TokenType.IDENTIFIER, "x", 1),
                                token(TokenType.PUNCTUATION, ":", 2))));
        assertFalse(parser.matches(bufferOf()));
    }

    @Test
    void matchesDoesNotConsumeTokens() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.NUMBER, "5", 5),
                        token(TokenType.PUNCTUATION, ";", 6));

        parser.matches(tokens);

        assertEquals(token(TokenType.IDENTIFIER, "x", 1), tokens.lookahead(0).orElseThrow());
    }

    @Test
    void parsesAnAssignmentWithALiteral() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.NUMBER, "5", 5),
                        token(TokenType.PUNCTUATION, ";", 6));

        Result<Statement> result = parser.parse(tokens);

        Statement expected =
                new AssignmentStatement(
                        new Identifier("x", range(1, 1)),
                        new NumberLiteral(5.0, range(5, 5)),
                        range(1, 6));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parsesAnAssignmentWithAnIdentifier() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.IDENTIFIER, "y", 5),
                        token(TokenType.PUNCTUATION, ";", 6));

        Result<Statement> result = parser.parse(tokens);

        Statement expected =
                new AssignmentStatement(
                        new Identifier("x", range(1, 1)),
                        new Identifier("y", range(5, 5)),
                        range(1, 6));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parsesAnAssignmentWithACompoundExpression() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.NUMBER, "2", 5),
                        token(TokenType.OPERATOR, "+", 6),
                        token(TokenType.NUMBER, "3", 7),
                        token(TokenType.PUNCTUATION, ";", 8));

        Result<Statement> result = parser.parse(tokens);

        Statement expected =
                new AssignmentStatement(
                        new Identifier("x", range(1, 1)),
                        new BinaryExpression(
                                new NumberLiteral(2.0, range(5, 5)),
                                Operator.PLUS,
                                new NumberLiteral(3.0, range(7, 7)),
                                range(5, 7)),
                        range(1, 8));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void missingEqualsFailsAtTheOffendingToken() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.NUMBER, "5", 3),
                        token(TokenType.PUNCTUATION, ";", 4));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(3, 3), failure.range());
    }

    @Test
    void missingSemicolonFailsAfterTheLastTokenOfTheExpression() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.NUMBER, "5", 5));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(5, 5), failure.range());
    }

    @Test
    void emptyExpressionAfterEqualsFails() {
        TokenBuffer tokens =
                bufferOf(
                        token(TokenType.IDENTIFIER, "x", 1),
                        token(TokenType.OPERATOR, "=", 3),
                        token(TokenType.PUNCTUATION, ";", 4));

        Result<Statement> result = parser.parse(tokens);

        Failure<Statement> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(4, 4), failure.range());
    }
}
