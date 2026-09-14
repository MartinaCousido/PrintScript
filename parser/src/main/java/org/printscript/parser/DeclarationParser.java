package org.printscript.parser;

import java.util.Map;
import java.util.Optional;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
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
        return header(tokens, declarator)
                .andThen(parts -> initializer(tokens, parts))
                .andThen(
                        parts ->
                                semicolon(tokens, parts)
                                        .map(end -> declaration(declarator, parts, end)));
    }

    // lo que toda declaración tiene sí o sí: nombre, ':' y tipo
    private Result<Parts> header(TokenBuffer tokens, Token declarator) {
        return name(tokens, declarator)
                .andThen(
                        name ->
                                colon(tokens, name)
                                        .andThen(colon -> type(tokens, colon))
                                        .map(type -> new Parts(name, type, Optional.empty())));
    }

    private Result<Token> name(TokenBuffer tokens, Token declarator) {
        return Tokens.expect(
                tokens,
                token -> token.type() == TokenType.IDENTIFIER,
                "Expected an identifier after '" + declarator.value() + "'",
                declarator.range());
    }

    private Result<Token> colon(TokenBuffer tokens, Token name) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ":"),
                "Expected ':' after the variable name",
                name.range());
    }

    private Result<Token> type(TokenBuffer tokens, Token colon) {
        return Tokens.expect(
                tokens,
                token -> token.type() == TokenType.KEYWORD || token.type() == TokenType.IDENTIFIER,
                "Expected a type annotation after ':'",
                colon.range());
    }

    // el '= expresión' es opcional: sin él la declaración queda igual, sin inicializador
    private Result<Parts> initializer(TokenBuffer tokens, Parts parts) {
        Optional<Token> assignment =
                tokens.lookahead(0).filter(token -> Tokens.isOperator(token, "="));
        if (assignment.isEmpty()) {
            return new Success<>(parts);
        }
        tokens.commit(1);
        return expressionBuilder
                .build(tokens, assignment.get().range())
                .map(parts::withInitializer);
    }

    private Result<Token> semicolon(TokenBuffer tokens, Parts parts) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ";"),
                "Expected ';' at the end of the declaration",
                parts.beforeSemicolon());
    }

    private Statement declaration(Token declarator, Parts parts, Token semicolon) {
        return declarators
                .get(declarator.value())
                .build(
                        new Identifier(parts.name().value(), parts.name().range()),
                        new TypeName(parts.type().value(), parts.type().range()),
                        parts.initializer(),
                        new Range(declarator.range().start(), semicolon.range().end()));
    }

    // las piezas que se van juntando mientras se recorre la declaración
    private record Parts(Token name, Token type, Optional<Expression> initializer) {

        Parts withInitializer(Expression expression) {
            return new Parts(name, type, Optional.of(expression));
        }

        // dónde termina lo parseado hasta ahora, para que el error del ';' apunte ahí
        Range beforeSemicolon() {
            return initializer.map(Expression::range).orElseGet(type::range);
        }
    }
}
