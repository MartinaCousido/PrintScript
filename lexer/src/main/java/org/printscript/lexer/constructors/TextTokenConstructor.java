package org.printscript.lexer.constructors;

import java.util.Optional;
import org.printscript.common.Position;
import org.printscript.token.Token;

/** Bridges the original String API to constructors that can read source views. */
abstract class TextTokenConstructor implements TokenConstructor {
    @Override
    public final Optional<Token> tryBuild(String rest, Position start) {
        return tryBuild((CharSequence) rest, start);
    }

    @Override
    public abstract Optional<Token> tryBuild(CharSequence rest, Position start);
}
