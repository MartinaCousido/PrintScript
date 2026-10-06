package org.printscript.parser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.printscript.token.Token;

// el criterio de PrintScript 1.1, que agrega bloques entre llaves
// la sentencia termina cuando aparece un ';' en profundidad 0, o cuando una '}' devuelve
// la profundidad de 1 a 0. Un programa sin llaves nunca sale de profundidad 0, así que
// para 1.0 se comporta igual que SemicolonDelimiter.
public final class BlockAwareDelimiter implements StatementDelimiter {

    @Override
    public boolean endsStatement(
            List<Token> accumulated, Token current, Supplier<Optional<Token>> next) {
        int depth = depthOf(accumulated);
        if (Tokens.isPunctuation(current, ";")) {
            return depth == 0;
        }
        if (!Tokens.isPunctuation(current, "}")) {
            return false;
        }
        if (depth == 0) {
            // una llave suelta: se corta acá para que el parser la reporte como
            // sentencia irreconocible en vez de tragarse el resto del archivo
            return true;
        }
        // solo acá se consulta el token siguiente, que cuesta una línea más de fuente:
        // un 'else' continúa la misma sentencia, y con eso 'else if' sale gratis
        boolean closesOutermostBlock = depth == 1;
        return closesOutermostBlock && !isElse(next.get());
    }

    @Override
    public String unterminatedMessage(List<Token> accumulated) {
        return depthOf(accumulated) > 0
                ? "Expected '}' before the end of the stream"
                : "Expected ';' before the end of the stream";
    }

    private static boolean isElse(Optional<Token> token) {
        return token.filter(candidate -> Tokens.isKeyword(candidate, "else")).isPresent();
    }

    private static int depthOf(List<Token> accumulated) {
        int depth = 0;
        for (Token token : accumulated) {
            if (Tokens.isPunctuation(token, "{")) {
                depth = depth + 1;
            } else if (Tokens.isPunctuation(token, "}")) {
                depth = depth - 1;
            }
        }
        return depth;
    }
}
