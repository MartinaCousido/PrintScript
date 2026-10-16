package org.printscript.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.ast.Block;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.Statement;
import org.printscript.common.ErrorCode;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;

/** Parses conditional blocks with explicit continuations, independent of JVM stack depth. */
public final class IfParser implements StatementParser {

    private final ExpressionBuilder expressionBuilder;
    private final Supplier<List<StatementParser>> statementParsers;

    // los parsers llegan como Supplier porque la lista se contiene a sí misma: el if es
    // uno de los parsers que puede aparecer adentro de su propio cuerpo
    public IfParser(
            ExpressionBuilder expressionBuilder, Supplier<List<StatementParser>> statementParsers) {
        this.expressionBuilder = expressionBuilder;
        this.statementParsers = statementParsers;
    }

    @Override
    public boolean matches(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isKeyword(token, "if")).isPresent();
    }

    @Override
    public Result<Statement> parse(TokenBuffer tokens) {
        return new ParseSession(tokens).parse();
    }

    private final class ParseSession {
        private final TokenBuffer tokens;
        private final Deque<Runnable> continuations = new ArrayDeque<>();
        private Statement completed;
        private Failure<Statement> failure;

        private ParseSession(TokenBuffer tokens) {
            this.tokens = tokens;
        }

        private Result<Statement> parse() {
            continuations.push(this::parseConditional);
            while (failure == null && !continuations.isEmpty()) {
                continuations.pop().run();
            }
            return failure == null ? new Success<>(completed) : failure;
        }

        private void fail(Failure<?> error) {
            failure = new Failure<>(error.code(), error.message(), error.range());
        }

        private void parseConditional() {
            Token keyword = tokens.lookahead(0).orElseThrow();
            tokens.commit(1);
            Result<Expression> parsed = condition(tokens, keyword);
            if (parsed instanceof Failure<Expression> error) {
                fail(error);
                return;
            }
            Expression expression = ((Success<Expression>) parsed).value();
            continuations.push(() -> finishConditional(keyword, expression, (Block) completed));
            startBlock(expression.range());
        }

        private void finishConditional(Token keyword, Expression expression, Block thenBlock) {
            if (!startsElse(tokens)) {
                completed =
                        new IfStatement(
                                expression,
                                thenBlock,
                                Optional.empty(),
                                new Range(keyword.range().start(), thenBlock.range().end()));
                return;
            }
            Token elseKeyword = tokens.lookahead(0).orElseThrow();
            tokens.commit(1);
            continuations.push(
                    () ->
                            completed =
                                    new IfStatement(
                                            expression,
                                            thenBlock,
                                            Optional.of(completed),
                                            new Range(
                                                    keyword.range().start(),
                                                    completed.range().end())));
            if (matches(tokens)) {
                continuations.push(this::parseConditional);
            } else {
                startBlock(elseKeyword.range());
            }
        }

        private void startBlock(Range previous) {
            Result<Token> opening = openBrace(tokens, previous);
            if (opening instanceof Failure<Token> error) {
                fail(error);
                return;
            }
            Token open = ((Success<Token>) opening).value();
            List<Statement> statements = new ArrayList<>();
            continuations.push(() -> nextInBlock(open, statements));
        }

        private void nextInBlock(Token open, List<Statement> statements) {
            if (startsClosingBrace(tokens)) {
                Token close = tokens.lookahead(0).orElseThrow();
                tokens.commit(1);
                completed =
                        new Block(statements, new Range(open.range().start(), close.range().end()));
                return;
            }
            Optional<Token> next = tokens.lookahead(0);
            if (next.isEmpty()) {
                failure =
                        new Failure<>(
                                ErrorCode.UNCLOSED_BLOCK,
                                "Expected '}' to close the block",
                                open.range());
                return;
            }
            Optional<StatementParser> matching =
                    statementParsers.get().stream()
                            .filter(parser -> parser.matches(tokens))
                            .findFirst();
            if (matching.isPresent()) {
                parseInBlock(matching.get(), open, statements);
                return;
            }
            failure =
                    new Failure<>(
                            ErrorCode.UNRECOGNIZED_STATEMENT,
                            "Unrecognized statement",
                            next.get().range());
        }

        private void parseInBlock(StatementParser parser, Token open, List<Statement> statements) {
            if (IfParser.this.equals(parser)) {
                continuations.push(
                        () -> {
                            statements.add(completed);
                            continuations.push(() -> nextInBlock(open, statements));
                        });
                continuations.push(this::parseConditional);
            } else {
                Result<Statement> parsed = parser.parse(tokens);
                if (parsed instanceof Failure<Statement> error) {
                    fail(error);
                } else {
                    statements.add(((Success<Statement>) parsed).value());
                    continuations.push(() -> nextInBlock(open, statements));
                }
            }
        }
    }

    private Result<Expression> condition(TokenBuffer tokens, Token keyword) {
        return openParenthesis(tokens, keyword)
                .andThen(open -> expressionBuilder.build(tokens, open.range()))
                .andThen(condition -> closeParenthesis(tokens, condition).map(end -> condition));
    }

    private Result<Token> openParenthesis(TokenBuffer tokens, Token keyword) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, "("),
                ErrorCode.EXPECTED_OPENING_PARENTHESIS,
                "Expected '(' after 'if'",
                keyword.range());
    }

    private Result<Token> closeParenthesis(TokenBuffer tokens, Expression condition) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ")"),
                ErrorCode.EXPECTED_CLOSING_PARENTHESIS,
                "Expected ')' after the if condition",
                condition.range());
    }

    private Result<Token> openBrace(TokenBuffer tokens, Range previous) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, "{"),
                ErrorCode.EXPECTED_OPENING_BRACE,
                "Expected '{' to open the block",
                previous);
    }

    private static boolean startsElse(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isKeyword(token, "else")).isPresent();
    }

    private static boolean startsClosingBrace(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isPunctuation(token, "}")).isPresent();
    }
}
