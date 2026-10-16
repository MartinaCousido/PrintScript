package org.printscript.parser;

import java.util.ArrayDeque;
import java.util.Deque;
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
// arma todos los nodos de expresión: numberLiteral, stringLiteral, etc.
public final class ExpressionBuilder {

    private final Map<String, ReadFactory> reads;

    // 1.0 no tiene funciones de lectura: la tabla vacía es la versión sin ellas
    public ExpressionBuilder() {
        this(Map.of());
    }

    public ExpressionBuilder(Map<String, ReadFactory> reads) {
        this.reads = Map.copyOf(reads);
    }

    /** Parses nested expressions with heap frames instead of the JVM call stack. */
    public Result<Expression> build(TokenBuffer tokens, Range anchor) {
        return new BuildSession(tokens, anchor).parse();
    }

    private final class BuildSession {
        private final TokenBuffer tokens;
        private final Deque<ExpressionFrame> frames = new ArrayDeque<>();
        private Result<Expression> completed;

        private BuildSession(TokenBuffer tokens, Range anchor) {
            this.tokens = tokens;
            frames.push(new ExpressionFrame(anchor, null, null));
        }

        private Result<Expression> parse() {
            while (completed == null) {
                ExpressionFrame frame = frames.peek();
                Optional<Token> next = tokens.lookahead(0);
                if (frame.needsOperand) {
                    operand(frame, next);
                } else {
                    Optional<Operator> operator = next.flatMap(OperatorPrecedence::operatorFor);
                    if (operator.isPresent()) {
                        frame.pushOperator(operator.get());
                        frame.anchor = next.orElseThrow().range();
                        tokens.commit(1);
                    } else {
                        finish(frame, next);
                    }
                }
            }
            return completed;
        }

        private void operand(ExpressionFrame frame, Optional<Token> next) {
            if (next.isEmpty()) {
                completed = new Failure<>("Missing operand", frame.anchor);
                return;
            }
            Token token = next.get();
            boolean group = Tokens.isPunctuation(token, "(");
            boolean read = token.type() == TokenType.KEYWORD && reads.containsKey(token.value());
            if (group || read) {
                enter(token, read);
                return;
            }
            Result<Expression> operand = simpleOperand(token);
            if (operand instanceof Failure<Expression>) {
                completed = operand;
            } else {
                tokens.commit(1);
                frame.accept(((Success<Expression>) operand).value());
            }
        }

        private void enter(Token token, boolean read) {
            tokens.commit(1);
            Token open = token;
            if (read) {
                Result<Token> opening = openParenthesis(tokens, token);
                if (opening instanceof Failure<Token> failure) {
                    completed = new Failure<>(failure.message(), failure.range());
                    return;
                }
                open = ((Success<Token>) opening).value();
            }
            frames.push(new ExpressionFrame(open.range(), open, read ? token : null));
        }

        private void finish(ExpressionFrame frame, Optional<Token> next) {
            Expression expression = frame.finish();
            if (frame.open == null) {
                completed = new Success<>(expression);
                return;
            }
            Result<Expression> closed = close(frame, expression, next);
            if (closed instanceof Failure<Expression>) {
                completed = closed;
            } else {
                frames.pop();
                frames.peek().accept(((Success<Expression>) closed).value());
            }
        }

        private Result<Expression> close(
                ExpressionFrame frame, Expression expression, Optional<Token> next) {
            if (frame.keyword != null) {
                return closeParenthesis(tokens, frame.keyword, expression)
                        .map(closing -> read(frame.keyword, expression, closing));
            }
            if (next.isEmpty()) {
                return new Failure<>("Unclosed parenthesis", frame.open.range());
            }
            Token closing = next.get();
            if (!Tokens.isPunctuation(closing, ")")) {
                return new Failure<>("Expected ')'", closing.range());
            }
            tokens.commit(1);
            return new Success<>(
                    withRange(
                            expression,
                            new Range(frame.open.range().start(), closing.range().end())));
        }
    }

    private static final class ExpressionFrame {
        private final Deque<Expression> operands = new ArrayDeque<>();
        private final Deque<Operator> operators = new ArrayDeque<>();
        private final Token open;
        private final Token keyword;
        private Range anchor;
        private boolean needsOperand = true;

        private ExpressionFrame(Range anchor, Token open, Token keyword) {
            this.anchor = anchor;
            this.open = open;
            this.keyword = keyword;
        }

        private void accept(Expression expression) {
            operands.push(expression);
            needsOperand = false;
        }

        private void pushOperator(Operator operator) {
            while (!operators.isEmpty()
                    && OperatorPrecedence.precedenceOf(operators.peek())
                            >= OperatorPrecedence.precedenceOf(operator)) {
                reduce();
            }
            operators.push(operator);
            needsOperand = true;
        }

        private void reduce() {
            Expression right = operands.pop();
            Expression left = operands.pop();
            operands.push(combine(left, operators.pop(), right));
        }

        private Expression finish() {
            while (!operators.isEmpty()) {
                reduce();
            }
            return operands.pop();
        }
    }

    private static Result<Expression> simpleOperand(Token token) {
        return switch (token.type()) {
            case NUMBER -> numberLiteral(token);
            case STRING -> stringLiteral(token);
            case BOOLEAN -> booleanLiteral(token);
            case IDENTIFIER -> identifier(token);
            default -> unexpectedToken(token);
        };
    }

    private static Expression combine(Expression left, Operator operator, Expression right) {
        return new BinaryExpression(
                left, operator, right, new Range(left.range().start(), right.range().end()));
    }

    private static Result<Expression> stringLiteral(Token token) {
        return new Success<>(new StringLiteral(token.value(), token.range()));
    }

    private static Result<Expression> booleanLiteral(Token token) {
        return new Success<>(
                new BooleanLiteral(Boolean.parseBoolean(token.value()), token.range()));
    }

    private static Result<Expression> identifier(Token token) {
        return new Success<>(new Identifier(token.value(), token.range()));
    }

    private static Result<Token> openParenthesis(TokenBuffer tokens, Token keyword) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, "("),
                "Expected '(' after '" + keyword.value() + "'",
                keyword.range());
    }

    private static Result<Token> closeParenthesis(
            TokenBuffer tokens, Token keyword, Expression argument) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ")"),
                "Expected ')' after the argument of '" + keyword.value() + "'",
                argument.range());
    }

    private Expression read(Token keyword, Expression argument, Token close) {
        return reads.get(keyword.value())
                .build(argument, new Range(keyword.range().start(), close.range().end()));
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
