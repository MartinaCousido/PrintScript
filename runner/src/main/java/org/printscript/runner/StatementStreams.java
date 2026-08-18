package org.printscript.runner;

import java.util.Iterator;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.providers.LexerVersionProvider;
import org.printscript.parser.Parser;
import org.printscript.parser.TokenBuffer;
import org.printscript.parser.providers.ParserVersionProvider;

final class StatementStreams {

    private StatementStreams() {
    }

    static Parser statementStream(Iterator<String> lines, String version) {
        Lexer lexer = new LexerVersionProvider().providerFor(version).provide(lines);
        TokenBuffer buffer = new TokenBuffer(lexer);
        return new ParserVersionProvider().providerFor(version).provide(buffer);
    }
}
