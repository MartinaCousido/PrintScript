package org.printscript.parser;

import java.util.Map;
import java.util.Optional;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.StringLiteral;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// arma las cuentas respetando que debe resolverse primero
public final class ExpressionBuilder {

    private final Map<String, ReadFactory> reads;

    // 1.0 no tiene funciones de lectura: la tabla vacía es la versión sin ellas
    public ExpressionBuilder() {
        this(Map.of());
    }

    public ExpressionBuilder(Map<String, ReadFactory> reads) {
        this.reads = Map.copyOf(reads);
    }

    public Result<Expression> build(TokenBuffer tokens, Range anchor) {
        return parseBinary(tokens, 0, anchor);
    }

    private Result<Expression> parseBinary(TokenBuffer tokens, int minPrecedence, Range anchor) {
        Result<Expression> leftResult = parsePrimary(tokens, anchor);
        if (!(leftResult instanceof Success<Expression>(Expression left))) {
            return leftResult;
        }
        while (true) {
            Optional<Token> operatorToken = tokens.lookahead(0);
            Optional<Operator> operator = operatorToken.flatMap(OperatorPrecedence::operatorFor);
            if (operator.isEmpty()
                    || OperatorPrecedence.precedenceOf(operator.get()) < minPrecedence) {
                return new Success<>(left);
            }
            tokens.commit(1);
            int rightMinPrecedence = OperatorPrecedence.precedenceOf(operator.get()) + 1;
            Result<Expression> rightResult =
                    parseBinary(tokens, rightMinPrecedence, operatorToken.get().range());
            if (!(rightResult instanceof Success<Expression>(Expression right))) {
                return rightResult;
            }
            left =
                    new BinaryExpression(
                            left,
                            operator.get(),
                            right,
                            new Range(left.range().start(), right.range().end()));
        }
    }

    private Result<Expression> parsePrimary(TokenBuffer tokens, Range anchor) {
        Optional<Token> next = tokens.lookahead(0);
        if (next.isEmpty()) {
            return new Failure<>("Missing operand", anchor);
        }
        Token token = next.get();
        return switch (token.type()) {
            case NUMBER -> {
                tokens.commit(1);
                yield numberLiteral(token);
            }
            case STRING -> {
                tokens.commit(1);
                yield new Success<>(new StringLiteral(token.value(), token.range()));
            }
            case BOOLEAN -> {
                tokens.commit(1);
                yield new Success<>(
                        new BooleanLiteral(Boolean.parseBoolean(token.value()), token.range()));
            }
            case IDENTIFIER -> {
                tokens.commit(1);
                yield new Success<>(new Identifier(token.value(), token.range()));
            }
            case PUNCTUATION -> parsePunctuation(tokens, token);
            case KEYWORD -> parseKeyword(tokens, token);
            default -> unexpectedToken(token);
        };
    }

    private Result<Expression> parsePunctuation(TokenBuffer tokens, Token token) {
        return "(".equals(token.value()) ? parseGrouped(tokens, token) : unexpectedToken(token);
    }

    // las funciones de lectura de la versión; en 1.0 la tabla está vacía y una keyword
    // en posición de operando es un error como cualquier otra
    private Result<Expression> parseKeyword(TokenBuffer tokens, Token token) {
        return reads.containsKey(token.value()) ? parseRead(tokens, token) : unexpectedToken(token);
    }

    // readInput("x") y readEnv("X") se parsean igual: keyword, paréntesis, un argumento
    private Result<Expression> parseRead(TokenBuffer tokens, Token keyword) {
        tokens.commit(1);
        Result<Token> openResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, "("),
                        "Expected '(' after '" + keyword.value() + "'",
                        keyword.range());
        if (openResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token open = ((Success<Token>) openResult).value();

        Result<Expression> argumentResult = parseBinary(tokens, 0, open.range());
        if (argumentResult instanceof Failure<Expression> failure) {
            return failure;
        }
        Expression argument = ((Success<Expression>) argumentResult).value();

        Result<Token> closeResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, ")"),
                        "Expected ')' after the argument of '" + keyword.value() + "'",
                        argument.range());
        if (closeResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token close = ((Success<Token>) closeResult).value();
        Range range = new Range(keyword.range().start(), close.range().end());
        return new Success<>(reads.get(keyword.value()).build(argument, range));
    }

    private Result<Expression> parseGrouped(TokenBuffer tokens, Token openParenthesis) {
        tokens.commit(1);
        Result<Expression> innerResult = parseBinary(tokens, 0, openParenthesis.range());
        if (!(innerResult instanceof Success<Expression>(Expression inner))) {
            return innerResult;
        }
        Optional<Token> close = tokens.lookahead(0);
        if (close.isEmpty()) {
            return new Failure<>("Unclosed parenthesis", openParenthesis.range());
        }
        if (close.get().type() != TokenType.PUNCTUATION || !")".equals(close.get().value())) {
            return new Failure<>("Expected ')'", close.get().range());
        }
        tokens.commit(1);
        Range grouped = new Range(openParenthesis.range().start(), close.get().range().end());
        return new Success<>(withRange(inner, grouped));
    }

    private static Expression withRange(Expression expression, Range range) {
        return switch (expression) {
            case NumberLiteral literal -> new NumberLiteral(literal.value(), range);
            case StringLiteral literal -> new StringLiteral(literal.value(), range);
            case BooleanLiteral literal -> new BooleanLiteral(literal.value(), range);
            case Identifier identifier -> new Identifier(identifier.name(), range);
            case BinaryExpression binary ->
                    new BinaryExpression(binary.left(), binary.operator(), binary.right(), range);
            case ReadInput read -> new ReadInput(read.prompt(), range);
            case ReadEnv read -> new ReadEnv(read.name(), range);
        };
    }

    private static Result<Expression> numberLiteral(Token token) {
        try {
            return new Success<>(
                    new NumberLiteral(Double.parseDouble(token.value()), token.range()));
        } catch (NumberFormatException e) {
            return new Failure<>("Invalid number literal '" + token.value() + "'", token.range());
        }
    }

    private static Result<Expression> unexpectedToken(Token token) {
        return new Failure<>(
                "Unexpected token '" + token.value() + "' in operand position", token.range());
    }
}
