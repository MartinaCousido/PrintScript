package org.printscript.parser;

public interface ParserProvider {

    Parser provide(TokenBuffer tokens);
}
