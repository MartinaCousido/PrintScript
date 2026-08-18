package org.printscript.parser.providers;

import org.printscript.parser.Parser;
import org.printscript.parser.TokenBuffer;

public interface ParserProvider {

    Parser provide(TokenBuffer tokens);
}
