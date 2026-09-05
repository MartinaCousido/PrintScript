package org.printscript.parser;

import java.util.Map;
import java.util.Optional;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// arma las declaraciones: let x: number = 12;  const y: string = "hola";
// va pidiendo pieza por pieza; si falta alguna, devuelve error
// qué declaradores existen no lo sabe esta clase: se los pasa el ParserFactory en una
// tabla de keyword a fábrica de nodo, así que sumar uno no le agrega ninguna rama
public final class DeclarationParser implements StatementParser {

    private final ExpressionBuilder expressionBuilder;
    private final Map<String, DeclarationFactory> declarators;

    public DeclarationParser(
            ExpressionBuilder expressionBuilder, Map<String, DeclarationFactory> declarators) {
        this.expressionBuilder = expressionBuilder;
        this.declarators = Map.copyOf(declarators);
    }

    @Override
    public boolean matches(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(this::isDeclarator).isPresent();
    }

    private boolean isDeclarator(Token token) {
        return declarators.keySet().stream().anyMatch(keyword -> Tokens.isKeyword(token, keyword));
    }

    @Override
    public Result<Statement> parse(TokenBuffer tokens) {
        Token declarator = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);

        Result<Token> nameResult =
                Tokens.expect(
                        tokens,
                        token -> token.type() == TokenType.IDENTIFIER,
                        "Expected an identifier after '" + declarator.value() + "'",
                        declarator.range());
        if (nameResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token name = ((Success<Token>) nameResult).value();

        Result<Token> colonResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, ":"),
                        "Expected ':' after the variable name",
                        name.range());
        if (colonResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token colon = ((Success<Token>) colonResult).value();

        Result<Token> typeResult =
                Tokens.expect(
                        tokens,
                        token ->
                                token.type() == TokenType.KEYWORD
                                        || token.type() == TokenType.IDENTIFIER,
                        "Expected a type annotation after ':'",
                        colon.range());
        if (typeResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token type = ((Success<Token>) typeResult).value();

        Optional<Token> assignment =
                tokens.lookahead(0).filter(token -> Tokens.isOperator(token, "="));
        Optional<Expression> initializer = Optional.empty();
        Range beforeSemicolon = type.range();
        if (assignment.isPresent()) {
            tokens.commit(1);
            Result<Expression> initializerResult =
                    expressionBuilder.build(tokens, assignment.get().range());
            if (initializerResult instanceof Failure<Expression> failure) {
                return Tokens.forward(failure);
            }
            Expression expression = ((Success<Expression>) initializerResult).value();
            initializer = Optional.of(expression);
            beforeSemicolon = expression.range();
        }

        Result<Token> semicolonResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, ";"),
                        "Expected ';' at the end of the declaration",
                        beforeSemicolon);
        if (semicolonResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token semicolon = ((Success<Token>) semicolonResult).value();

        return new Success<>(
                declarators
                        .get(declarator.value())
                        .build(
                                new Identifier(name.value(), name.range()),
                                new TypeName(type.value(), type.range()),
                                initializer,
                                new Range(declarator.range().start(), semicolon.range().end())));
    }
}
