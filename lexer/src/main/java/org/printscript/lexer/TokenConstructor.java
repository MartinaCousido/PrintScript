package org.printscript.lexer;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.token.Token;
//Lo usan todos los reconocedores. Recibe lo que queda de la linea y devuelve un token o nada si eso no es lo suyo
public interface TokenConstructor {

    Optional<Token> tryBuild(String rest, Position start);
}
