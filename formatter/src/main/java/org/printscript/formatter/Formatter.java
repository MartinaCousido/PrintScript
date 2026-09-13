package org.printscript.formatter;

import java.util.Iterator;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// Reescribe el fuente token por token, entregando de a un pedazo.
//
// No arma el árbol y no conoce la gramática: lo que hace falta para formatear es saber qué
// tokens hay y cuánto espacio va entre ellos. Por eso trabaja sobre el lexer y no sobre el
// parser, y por eso puede formatear código que no parsea.
public final class Formatter implements Iterator<Result<String>> {

    private final Iterator<Token> tokens;
    private final Gaps gaps;

    private Token previous;

    public Formatter(Iterator<Token> tokens, FormatterConfiguration configuration) {
        this.tokens = tokens;
        this.gaps = new Gaps(configuration);
    }

    @Override
    public boolean hasNext() {
        return tokens.hasNext();
    }

    @Override
    public Result<String> next() {
        Token token = tokens.next();
        if (token.type() == TokenType.UNKNOWN) {
            return new Failure<>("Símbolo no reconocido: '" + token.value() + "'", token.range());
        }
        String piece = space(token) + lexemeOf(token);
        gaps.advancePast(previous, token);
        previous = token;
        return new Success<>(piece);
    }

    // el lexer guarda el contenido del string sin las comillas, porque es lo que el
    // intérprete tiene que imprimir. Para reescribir el fuente hay que devolvérselas.
    private static String lexemeOf(Token token) {
        return token.type() == TokenType.STRING ? "\"" + token.value() + "\"" : token.value();
    }

    // el primer token del archivo no tiene nada antes: lo que traiga de sangría se
    // conserva tal cual porque nace de su propia posición
    private String space(Token token) {
        return previous == null ? "" : gaps.between(previous, token);
    }
}
