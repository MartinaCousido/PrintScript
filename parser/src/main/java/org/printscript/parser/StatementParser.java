package org.printscript.parser;

import org.printscript.ast.Statement;
import org.printscript.common.Result;

public interface StatementParser {

    boolean matches(TokenBuffer tokens);

    Result<Statement> parse(TokenBuffer tokens);
}
