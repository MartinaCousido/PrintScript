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

    // el mensaje cuando el fuente se termina antes de que la sentencia cierre
    // recibe lo acumulado porque una sentencia puede quedar abierta por más de un
    // motivo: falta el ';' o falta la '}' que cierra un bloque
    String unterminatedMessage(List<Token> accumulated);
}
