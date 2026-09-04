package org.printscript.semantic;

import org.printscript.ast.Statement;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;

// la etapa que valida que un programa sintácticamente correcto además tenga sentido
// envuelve al parser: consume Result<Statement> y produce Result<Statement>, así se
// encadena sin que el parser ni el intérprete sepan que existe
public final class SemanticAnalyzer implements PrintScriptIterator<Result<Statement>> {

    private final PrintScriptIterator<Result<Statement>> statements;

    public SemanticAnalyzer(PrintScriptIterator<Result<Statement>> statements) {
        this.statements = statements;
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    // sin reglas todavía la etapa es transparente; los errores de sintaxis viajan tal
    // cual, sin re-empaquetarse y sin perder su rango
    @Override
    public Result<Statement> getNext() {
        return statements.getNext();
    }
}
