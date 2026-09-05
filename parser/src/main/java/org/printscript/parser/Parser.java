package org.printscript.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// pieza principal del módulo
// entrega una sentencia por vez, como el lexer entrega un token por vez
public final class Parser implements Iterator<Result<Statement>> {

    private final TokenBuffer buffer;
    private final List<StatementParser> statementParsers;

    public Parser(TokenBuffer buffer, List<StatementParser> statementParsers) {
        this.buffer = buffer;
        this.statementParsers = List.copyOf(statementParsers);
    }

    @Override
    public boolean hasNext() {
        return buffer.lookahead(0).isPresent();
    }

    // junta tokens hasta encotrar un ;
    @Override
    public Result<Statement> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<Token> statement = new ArrayList<>();
        while (true) {
            Optional<Token> current = buffer.lookahead(statement.size());
            if (current.isEmpty()) {
                Token last = statement.getLast();
                buffer.commit(statement.size());
                return new Failure<>("Expected ';' before the end of the stream", last.range());
            }
            statement.add(current.get());
            if (isSemicolon(current.get())) {
                break;
            }
        }
        buffer.commit(statement.size());
        TokenBuffer statementTokens = new TokenBuffer(new ListTokenIterator(statement));
        for (StatementParser parser : statementParsers) {
            if (parser.matches(statementTokens)) {
                return parser.parse(statementTokens);
            }
        }
        return new Failure<>("Unrecognized statement", statement.getFirst().range());
    }

    private static boolean isSemicolon(Token token) {
        return token.type() == TokenType.PUNCTUATION && ";".equals(token.value());
    }
}
