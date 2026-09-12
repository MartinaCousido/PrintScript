package org.printscript.parser;

import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

public final class AssignmentParser implements StatementParser {

    private final ExpressionBuilder expressionBuilder;

    public AssignmentParser(ExpressionBuilder expressionBuilder) {
        this.expressionBuilder = expressionBuilder;
    }

    @Override
    public boolean matches(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> token.type() == TokenType.IDENTIFIER).isPresent()
                && tokens.lookahead(1).filter(token -> Tokens.isOperator(token, "=")).isPresent();
    }

    @Override
    public Result<Statement> parse(TokenBuffer tokens) {
        Token name = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);
        return equalsSign(tokens, name)
                .andThen(sign -> expressionBuilder.build(tokens, sign.range()))
                .andThen(
                        expression ->
                                semicolon(tokens, expression)
                                        .map(end -> assignment(name, expression, end)));
    }

    private Result<Token> equalsSign(TokenBuffer tokens, Token name) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isOperator(token, "="),
                "Expected '=' after the variable name",
                name.range());
    }

    private Result<Token> semicolon(TokenBuffer tokens, Expression expression) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ";"),
                "Expected ';' at the end of the assignment",
                expression.range());
    }

    private Statement assignment(Token name, Expression expression, Token semicolon) {
        return new AssignmentStatement(
                new Identifier(name.value(), name.range()),
                expression,
                new Range(name.range().start(), semicolon.range().end()));
    }
}
