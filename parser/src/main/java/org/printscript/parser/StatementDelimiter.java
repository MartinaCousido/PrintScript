package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

// dice dónde termina una sentencia
// es lo único del corte que cambia entre versiones del lenguaje, por eso vive afuera del Parser
public interface StatementDelimiter {

    // accumulated son los tokens de la sentencia anteriores a current
    // next es perezoso a propósito: pedirlo obliga al lexer a leer una línea más del fuente,
    // así que solo hay que pedirlo cuando de verdad hace falta para decidir
    boolean endsStatement(List<Token> accumulated, Token current, Supplier<Optional<Token>> next);

    // el mensaje de error cuando el fuente se termina antes de que la sentencia cierre
    String unterminatedMessage();
}
