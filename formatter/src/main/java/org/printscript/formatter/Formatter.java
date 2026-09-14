package org.printscript.formatter;

import java.util.Iterator;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.token.Token;
import org.printscript.token.TokenType;

// Reescribe el fuente token por token, entregando de a un pedazo.
// No arma el árbol y no conoce la gramática: lo que hace es saber qué
// tokens hay y cuánto espacio va entre ellos.
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
            // el símbolo no se emite, pero su posición sí avanza el estado: así el espacio
            // del token siguiente se mide contra él y no contra el token anterior
            gaps.advancePast(previous, token);
            previous = token;
            return new Failure<>("Símbolo no reconocido: '" + token.value() + "'", token.range());
        }
        String piece = space(token) + lexemeOf(token);
        gaps.advancePast(previous, token);
        previous = token;
        return new Success<>(piece);
    }

    // el lexer guarda el contenido del string sin las comillas, porque es lo que el
    // intérprete tiene que imprimir. Para reescribir el fuente hay que devolvérselas.
    // La comilla original se perdió, pero un literal sólo puede contener la comilla que no
    // lo delimita, así que se reescribe con la que no aparece adentro: así una cadena que
    // venía con comillas simples y contiene una doble no sale como código inválido.
    private static String lexemeOf(Token token) {
        if (token.type() != TokenType.STRING) {
            return token.value();
        }
        char quote = token.value().indexOf('"') >= 0 ? '\'' : '"';
        return quote + token.value() + quote;
    }

    private String space(Token token) {
        return previous == null ? "" : gaps.between(previous, token);
    }
}
