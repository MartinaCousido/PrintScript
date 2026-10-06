package org.printscript.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

class ExpressionBuilderTest {

    private final ExpressionBuilder builder = new ExpressionBuilder();

    private static Range range(int startColumn, int endColumn) {
        return new Range(new Position(1, startColumn), new Position(1, endColumn));
    }

    private static Token number(String value, int column) {
        return new Token(TokenType.NUMBER, value, range(column, column + value.length() - 1));
    }

    private static Token operator(String value, int column) {
        return new Token(TokenType.OPERATOR, value, range(column, column));
    }

    private static Token punctuation(String value, int column) {
        return new Token(TokenType.PUNCTUATION, value, range(column, column));
    }

    private static Token string(String value, int column) {
        return new Token(TokenType.STRING, value, range(column, column + value.length() + 1));
    }

    private static Token identifier(String value, int column) {
        return new Token(TokenType.IDENTIFIER, value, range(column, column + value.length() - 1));
    }

    private static TokenBuffer bufferOf(Token... tokens) {
        return new TokenBuffer(new ListTokenIterator(List.of(tokens)));
    }

    @Test
    void multiplicationBindsTighterThanAddition() {
        TokenBuffer tokens =
                bufferOf(
                        number("2", 1),
                        operator("+", 3),
                        number("3", 5),
                        operator("*", 7),
                        number("4", 9));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Expression expected =
                new BinaryExpression(
                        new NumberLiteral(2.0, range(1, 1)),
                        Operator.PLUS,
                        new BinaryExpression(
                                new NumberLiteral(3.0, range(5, 5)),
                                Operator.MULTIPLY,
                                new NumberLiteral(4.0, range(9, 9)),
                                range(5, 9)),
                        range(1, 9));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void parenthesesGroupAgainstPrecedence() {
        TokenBuffer tokens =
                bufferOf(
                        punctuation("(", 1),
                        number("2", 2),
                        operator("+", 4),
                        number("3", 6),
                        punctuation(")", 7),
                        operator("*", 9),
                        number("4", 11));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Expression expected =
                new BinaryExpression(
                        new BinaryExpression(
                                new NumberLiteral(2.0, range(2, 2)),
                                Operator.PLUS,
                                new NumberLiteral(3.0, range(6, 6)),
                                range(1, 7)),
                        Operator.MULTIPLY,
                        new NumberLiteral(4.0, range(11, 11)),
                        range(1, 11));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void subtractionIsLeftAssociative() {
        TokenBuffer tokens =
                bufferOf(
                        number("10", 1),
                        operator("-", 4),
                        number("2", 6),
                        operator("-", 8),
                        number("3", 10));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Expression expected =
                new BinaryExpression(
                        new BinaryExpression(
                                new NumberLiteral(10.0, range(1, 2)),
                                Operator.MINUS,
                                new NumberLiteral(2.0, range(6, 6)),
                                range(1, 6)),
                        Operator.MINUS,
                        new NumberLiteral(3.0, range(10, 10)),
                        range(1, 10));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void concatenationCombinesStringsAndIdentifiers() {
        TokenBuffer tokens =
                bufferOf(string("Hello ", 1), operator("+", 10), identifier("name", 12));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Expression expected =
                new BinaryExpression(
                        new StringLiteral("Hello ", range(1, 8)),
                        Operator.PLUS,
                        new Identifier("name", range(12, 15)),
                        range(1, 15));
        assertEquals(new Success<>(expected), result);
    }

    @Test
    void missingOperandAfterAnOperatorFailsAtTheOperator() {
        TokenBuffer tokens = bufferOf(number("2", 1), operator("+", 3));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Failure<Expression> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(3, 3), failure.range());
    }

    @Test
    void unclosedParenthesisFailsAtTheOpeningParenthesis() {
        TokenBuffer tokens =
                bufferOf(punctuation("(", 1), number("2", 2), operator("+", 4), number("3", 6));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Failure<Expression> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(1, 1), failure.range());
    }

    @Test
    void unexpectedTokenInOperandPositionFailsAtThatToken() {
        TokenBuffer tokens =
                bufferOf(number("2", 1), operator("+", 3), operator("*", 5), number("3", 7));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Failure<Expression> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(5, 5), failure.range());
    }

    @Test
    void malformedNumberLiteralFailsAtThatToken() {
        TokenBuffer tokens = bufferOf(number("1.2.3", 1));

        Result<Expression> result = builder.build(tokens, range(1, 1));

        Failure<Expression> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(1, 5), failure.range());
    }

    @Test
    void emptyExpressionFailsAtTheAnchor() {
        TokenBuffer tokens = bufferOf();

        Result<Expression> result = builder.build(tokens, range(9, 9));

        Failure<Expression> failure = assertInstanceOf(Failure.class, result);
        assertEquals(range(9, 9), failure.range());
    }
}
