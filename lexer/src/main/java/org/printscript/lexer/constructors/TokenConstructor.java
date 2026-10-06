package org.printscript.lexer.constructors;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.token.Token;

// Lo usan todos los reconocedores. Recibe lo que queda de la linea y devuelve un token o nada si
// eso no es lo suyo
@FunctionalInterface
public interface TokenConstructor {

    /** Returns a token starting at start, spanning at least one source character on that line. */
    Optional<Token> tryBuild(String rest, Position start);

    /** Allows zero-copy source slices. Existing constructors keep their String contract. */
    default Optional<Token> tryBuild(CharSequence rest, Position start) {
        return tryBuild(rest.toString(), start);
    }
}
