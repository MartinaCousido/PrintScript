package org.printscript.parser;

import org.printscript.ast.Statement;
import org.printscript.common.Result;
//parser de un tipo de sentencia
public interface StatementParser {

    //dice si le toca
    boolean matches(TokenBuffer tokens);

    Result<Statement> parse(TokenBuffer tokens); //parse la arma
}
