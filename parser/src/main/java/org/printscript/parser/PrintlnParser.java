package org.printscript.parser;

import org.printscript.ast.Expression;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.ErrorCode;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.token.Token;

// arma los println pidiendo pieza por pieza
public final class PrintlnParser implements StatementParser {

    private final ExpressionBuilder expressionBuilder;

    public PrintlnParser(ExpressionBuilder expressionBuilder) {
        this.expressionBuilder = expressionBuilder;
    }

    @Override
    public boolean matches(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isKeyword(token, "println")).isPresent();
    }

    @Override
    public Result<Statement> parse(TokenBuffer tokens) {
        Token println = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);
        return open(tokens, println)
                .andThen(open -> expressionBuilder.build(tokens, open.range()))
                .andThen(
                        argument ->
                                close(tokens, argument)
                                        .andThen(close -> semicolon(tokens, close))
                                        .map(end -> println(println, argument, end)));
    }

    private Result<Token> open(TokenBuffer tokens, Token println) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, "("),
                ErrorCode.EXPECTED_OPENING_PARENTHESIS,
                "Expected '(' after 'println'",
                println.range());
    }

    private Result<Token> close(TokenBuffer tokens, Expression argument) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ")"),
                ErrorCode.EXPECTED_CLOSING_PARENTHESIS,
                "Expected ')' after the println argument",
                argument.range());
    }

    private Result<Token> semicolon(TokenBuffer tokens, Token close) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ";"),
                ErrorCode.MISSING_SEMICOLON,
                "Expected ';' at the end of the println statement",
                close.range());
    }

    private Statement println(Token println, Expression argument, Token semicolon) {
        return new PrintlnStatement(
                argument, new Range(println.range().start(), semicolon.range().end()));
    }
}
