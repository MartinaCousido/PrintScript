package org.printscript.lexer;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.token.Token;

public interface TokenConstructor {

    Optional<Token> tryBuild(String rest, Position start);
}
