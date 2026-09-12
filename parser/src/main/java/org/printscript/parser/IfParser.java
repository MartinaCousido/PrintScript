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
        return condition(tokens, keyword)
                .andThen(
                        condition ->
                                block(tokens, condition.range())
                                        .andThen(
                                                then ->
                                                        ifStatement(
                                                                tokens, keyword, condition, then)));
    }

    // el else es opcional, y de eso depende dónde termina el if entero
    private Result<Statement> ifStatement(
            TokenBuffer tokens, Token keyword, Expression condition, Block thenBlock) {
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
        return elseBranch(tokens, elseKeyword)
                .map(branch -> withElse(keyword, condition, thenBlock, branch));
    }

    private static IfStatement withElse(
            Token keyword, Expression condition, Block thenBlock, Statement branch) {
        return new IfStatement(
                condition,
                thenBlock,
                Optional.of(branch),
                new Range(keyword.range().start(), branch.range().end()));
    }

    // else seguido de otro if es "else if": la rama es ese if, sin ningún Block que lo
    // envuelva, y eso es lo que después deja al formatter imprimirlo en una línea
    private Result<Statement> elseBranch(TokenBuffer tokens, Token elseKeyword) {
        if (matches(tokens)) {
            return parse(tokens);
        }
        return block(tokens, elseKeyword.range()).map(block -> (Statement) block);
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
                "Expected '(' after 'if'",
                keyword.range());
    }

    private Result<Token> closeParenthesis(TokenBuffer tokens, Expression condition) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, ")"),
                "Expected ')' after the if condition",
                condition.range());
    }

    private Result<Block> block(TokenBuffer tokens, Range previous) {
        return openBrace(tokens, previous).andThen(open -> statements(tokens, open));
    }

    private Result<Token> openBrace(TokenBuffer tokens, Range previous) {
        return Tokens.expect(
                tokens,
                token -> Tokens.isPunctuation(token, "{"),
                "Expected '{' to open the block",
                previous);
    }

    // el único lugar que sigue desarmando el Result a mano: es un bucle que acumula, y
    // ahí no hay cadena que encadenar
    private Result<Block> statements(TokenBuffer tokens, Token open) {
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
