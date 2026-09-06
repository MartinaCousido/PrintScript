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

final class StatementStreams {

    private StatementStreams() {}

    // lexer -> buffer -> parser
    // lo consumen el formatter y el linter, que trabajan sobre el árbol sin exigir que
    // el programa tenga sentido: se tiene que poder formatear y lintear código con
    // errores de tipos, que es justo cuando más falta hace
    static Iterator<Result<Statement>> statementStream(Iterator<String> lines, Version version) {
        Lexer lexer = LexerFactory.create(lines, version);
        TokenBuffer buffer = new TokenBuffer(lexer);
        return ParserFactory.create(buffer, version);
    }

    // la misma cadena más semantic
    // lo consumen validate y execute: es lo que hace que el modo Validation valide
    // semántica, y lo que le garantiza al Executor la precondición de recibir
    // sentencias ya validadas
    static Iterator<Result<Statement>> validatedStatementStream(
            Iterator<String> lines, Version version) {
        return SemanticFactory.create(statementStream(lines, version), version);
    }
}
