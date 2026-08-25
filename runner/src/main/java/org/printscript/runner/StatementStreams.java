package org.printscript.runner;

import java.util.Iterator;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.LexerFactory;
import org.printscript.parser.Parser;
import org.printscript.parser.ParserFactory;
import org.printscript.parser.TokenBuffer;

final class StatementStreams {

    private StatementStreams() {}

    static Parser statementStream(Iterator<String> lines) {
        Lexer lexer = LexerFactory.create(lines);
        TokenBuffer buffer = new TokenBuffer(lexer);
        return ParserFactory.create(buffer);
    }
}
