package org.printscript.semantic;

import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.common.Success;

// la etapa que valida que un programa sintácticamente correcto además tenga sentido
// envuelve al parser: consume Result<Statement> y produce Result<Statement>, así se
// encadena sin que el parser ni el intérprete sepan que existe
public final class SemanticAnalyzer implements PrintScriptIterator<Result<Statement>> {

    private final PrintScriptIterator<Result<Statement>> statements;
    private final StatementChecker checker;

    public SemanticAnalyzer(PrintScriptIterator<Result<Statement>> statements) {
        this.statements = statements;
        this.checker = new StatementChecker();
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    @Override
    public Result<Statement> getNext() {
        Result<Statement> result = statements.getNext();
        // los errores de sintaxis viajan tal cual: no se re-empaquetan ni pierden su rango
        if (!(result instanceof Success<Statement> success)) {
            return result;
        }
        Result<Void> checked = success.value().accept(checker);
        if (checked instanceof Failure<Void> failure) {
            return new Failure<>(failure.message(), failure.range());
        }
        return result;
    }
}
