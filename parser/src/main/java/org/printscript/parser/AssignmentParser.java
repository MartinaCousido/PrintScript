package org.printscript.parser;

import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
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

        Result<Token> equalsResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isOperator(token, "="),
                        "Expected '=' after the variable name",
                        name.range());
        if (equalsResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token equals = ((Success<Token>) equalsResult).value();

        Result<Expression> expressionResult = expressionBuilder.build(tokens, equals.range());
        if (expressionResult instanceof Failure<Expression> failure) {
            return Tokens.forward(failure);
        }
        Expression expression = ((Success<Expression>) expressionResult).value();

        Result<Token> semicolonResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, ";"),
                        "Expected ';' at the end of the assignment",
                        expression.range());
        if (semicolonResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token semicolon = ((Success<Token>) semicolonResult).value();

        return new Success<>(
                new AssignmentStatement(
                        new Identifier(name.value(), name.range()),
                        expression,
                        new Range(name.range().start(), semicolon.range().end())));
    }
}
