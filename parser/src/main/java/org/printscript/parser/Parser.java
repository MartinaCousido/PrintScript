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

// pieza principal del módulo
// entrega una sentencia por vez, como el lexer entrega un token por vez
public final class Parser implements Iterator<Result<Statement>> {

    private final TokenBuffer buffer;
    private final List<StatementParser> statementParsers;
    private final StatementDelimiter delimiter;

    public Parser(
            TokenBuffer buffer,
            List<StatementParser> statementParsers,
            StatementDelimiter delimiter) {
        this.buffer = buffer;
        this.statementParsers = List.copyOf(statementParsers);
        this.delimiter = delimiter;
    }

    @Override
    public boolean hasNext() {
        return buffer.lookahead(0).isPresent();
    }

    // junta los tokens de una sentencia; dónde termina lo decide el delimitador
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
                return new Failure<>(delimiter.unterminatedMessage(statement), last.range());
            }
            Token token = current.get();
            int position = statement.size();
            boolean ends =
                    delimiter.endsStatement(statement, token, () -> buffer.lookahead(position + 1));
            statement.add(token);
            if (ends) {
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
}
