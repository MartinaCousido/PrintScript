package org.printscript.runner;

import java.util.Iterator;
import org.printscript.ast.Statement;
import org.printscript.common.Result;
import org.printscript.common.Version;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.LexerFactory;
import org.printscript.parser.ParserFactory;
import org.printscript.parser.TokenBuffer;
import org.printscript.semantic.SemanticFactory;

// arma las cadenas
final class StatementStreams {

    private StatementStreams() {}

    // lexer -> buffer -> parser
    // lo consumen el formatter y el linter, que trabajan sobre el árbol
    static Iterator<Result<Statement>> statementStream(Iterator<String> lines, Version version) {
        Lexer lexer = LexerFactory.create(lines, version);
        TokenBuffer buffer = new TokenBuffer(lexer);
        return ParserFactory.create(buffer, version);
    }

    // la misma cadena más semantic
    // lo consume validate y execute ya que le garantiza al Executor recibir sentencias ya validadas
    static Iterator<Result<Statement>> validatedStatementStream(
            Iterator<String> lines, Version version) {
        return SemanticFactory.create(statementStream(lines, version), version);
    }
}
