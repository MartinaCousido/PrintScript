package org.printscript.lexer;

import java.util.Iterator;

public interface LexerProvider {

    Lexer provide(Iterator<String> lines);
}
