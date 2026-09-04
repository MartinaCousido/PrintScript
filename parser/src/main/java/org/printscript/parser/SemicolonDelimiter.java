package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

// el criterio de PrintScript 1.0: la sentencia termina en el primer ';'
// no mira el token siguiente, así que no fuerza ninguna lectura extra del fuente
public final class SemicolonDelimiter implements StatementDelimiter {

    @Override
    public boolean endsStatement(
            List<Token> accumulated, Token current, Supplier<Optional<Token>> next) {
        return Tokens.isPunctuation(current, ";");
    }

    @Override
    public String unterminatedMessage() {
        return "Expected ';' before the end of the stream";
    }
}
