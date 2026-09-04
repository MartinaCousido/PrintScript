package org.printscript.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.ast.Block;
import org.printscript.ast.Expression;
import org.printscript.ast.IfStatement;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.token.Token;

// arma el if con su bloque y su else opcional
// el cuerpo se parsea con los mismos StatementParser que el nivel de arriba: lo que es
// una sentencia válida afuera lo es adentro, y no hay gramática duplicada. El
// anidamiento lo resuelve la recursión, no la aritmética de índices: nunca se busca "la
// primera llave que cierra", que es como se rompen los if anidados.
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
        Token keyword = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);

        Result<Expression> conditionResult = condition(tokens, keyword);
        if (conditionResult instanceof Failure<Expression> failure) {
            return Tokens.forward(failure);
        }
        Expression condition = ((Success<Expression>) conditionResult).value();

        Result<Block> thenResult = block(tokens, condition.range());
        if (thenResult instanceof Failure<Block> failure) {
            return Tokens.forward(failure);
        }
        Block thenBlock = ((Success<Block>) thenResult).value();

        if (!startsElse(tokens)) {
            return new Success<>(
                    new IfStatement(
                            condition,
                            thenBlock,
                            Optional.empty(),
                            new Range(keyword.range().start(), thenBlock.range().end())));
        }
        Token elseKeyword = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);

        Result<Statement> elseResult = elseBranch(tokens, elseKeyword);
        if (elseResult instanceof Failure<Statement> failure) {
            return failure;
        }
        Statement elseBranch = ((Success<Statement>) elseResult).value();
        return new Success<>(
                new IfStatement(
                        condition,
                        thenBlock,
                        Optional.of(elseBranch),
                        new Range(keyword.range().start(), elseBranch.range().end())));
    }

    // else seguido de otro if es "else if": la rama es ese if, sin ningún Block que lo
    // envuelva, y eso es lo que después deja al formatter imprimirlo en una línea
    private Result<Statement> elseBranch(TokenBuffer tokens, Token elseKeyword) {
        if (matches(tokens)) {
            return parse(tokens);
        }
        Result<Block> blockResult = block(tokens, elseKeyword.range());
        if (blockResult instanceof Failure<Block> failure) {
            return Tokens.forward(failure);
        }
        return new Success<>(((Success<Block>) blockResult).value());
    }

    private Result<Expression> condition(TokenBuffer tokens, Token keyword) {
        Result<Token> openResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, "("),
                        "Expected '(' after 'if'",
                        keyword.range());
        if (openResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token open = ((Success<Token>) openResult).value();

        Result<Expression> conditionResult = expressionBuilder.build(tokens, open.range());
        if (conditionResult instanceof Failure<Expression> failure) {
            return failure;
        }
        Expression condition = ((Success<Expression>) conditionResult).value();

        Result<Token> closeResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, ")"),
                        "Expected ')' after the if condition",
                        condition.range());
        if (closeResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        return new Success<>(condition);
    }

    private Result<Block> block(TokenBuffer tokens, Range previous) {
        Result<Token> openResult =
                Tokens.expect(
                        tokens,
                        token -> Tokens.isPunctuation(token, "{"),
                        "Expected '{' to open the block",
                        previous);
        if (openResult instanceof Failure<Token> failure) {
            return Tokens.forward(failure);
        }
        Token open = ((Success<Token>) openResult).value();

        List<Statement> statements = new ArrayList<>();
        while (!startsClosingBrace(tokens)) {
            Result<Statement> statement = nextStatement(tokens, open.range());
            if (statement instanceof Failure<Statement> failure) {
                return Tokens.forward(failure);
            }
            statements.add(((Success<Statement>) statement).value());
        }
        Token close = tokens.lookahead(0).orElseThrow();
        tokens.commit(1);
        return new Success<>(
                new Block(statements, new Range(open.range().start(), close.range().end())));
    }

    // el error de una sentencia de adentro viaja como dato y con su propio rango: el if
    // entero falla apuntando a la sentencia ofensora, no a la llave
    private Result<Statement> nextStatement(TokenBuffer tokens, Range open) {
        Optional<Token> next = tokens.lookahead(0);
        if (next.isEmpty()) {
            return new Failure<>("Expected '}' to close the block", open);
        }
        for (StatementParser parser : statementParsers.get()) {
            if (parser.matches(tokens)) {
                return parser.parse(tokens);
            }
        }
        return new Failure<>("Unrecognized statement", next.get().range());
    }

    private static boolean startsElse(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isKeyword(token, "else")).isPresent();
    }

    private static boolean startsClosingBrace(TokenBuffer tokens) {
        return tokens.lookahead(0).filter(token -> Tokens.isPunctuation(token, "}")).isPresent();
    }
}
