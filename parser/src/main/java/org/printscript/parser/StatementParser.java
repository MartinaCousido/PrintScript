package org.printscript.parser;

import org.printscript.ast.Statement;
import org.printscript.common.Result;

// parser de un tipo de sentencia
public interface StatementParser {

    /** May inspect tokens without committing them; reads only enough to recognize the statement. */
    boolean matches(TokenBuffer tokens);

    /**
     * Parses after matches returns true. Syntax errors are returned as Failure with their range.
     */
    Result<Statement> parse(TokenBuffer tokens);
}
