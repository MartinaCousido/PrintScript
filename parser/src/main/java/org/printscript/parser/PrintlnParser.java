package org.printscript.parser;

import org.printscript.ast.Expression;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
//arma los println pidiendo pieza por pieza
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

        Result<Token> openResult = Tokens.expect(
            tokens,
            token -> Tokens.isPunctuation(token, "("),
            "Expected '(' after 'println'",
            println.range());
        if (openResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token open = ((Success<Token>) openResult).value();

        Result<Expression> argumentResult = expressionBuilder.build(tokens, open.range());
        if (argumentResult instanceof Failure<Expression> failure) {
            return Tokens.forward(failure);
        }
        Expression argument = ((Success<Expression>) argumentResult).value();

        Result<Token> closeResult = Tokens.expect(
            tokens,
            token -> Tokens.isPunctuation(token, ")"),
            "Expected ')' after the println argument",
            argument.range());
        if (closeResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token close = ((Success<Token>) closeResult).value();

        Result<Token> semicolonResult = Tokens.expect(
            tokens,
            token -> Tokens.isPunctuation(token, ";"),
            "Expected ';' at the end of the println statement",
            close.range());
        if (semicolonResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token semicolon = ((Success<Token>) semicolonResult).value();

        return new Success<>(new PrintlnStatement(
            argument,
            new Range(println.range().start(), semicolon.range().end())));
    }
}
