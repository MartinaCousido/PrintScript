package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

// dice dónde termina una sentencia
// es lo único del corte que cambia entre versiones del lenguaje, por eso vive afuera del Parser
public interface StatementDelimiter {

    /** Returns a fresh scanner for one statement; stateless delimiters may return themselves. */
    default StatementDelimiter forStatement() {
        return this;
    }

    boolean endsStatement(List<Token> accumulated, Token current, Supplier<Optional<Token>> next);

    String unterminatedMessage(List<Token> accumulated);
}
