package org.printscript.semantic;

import java.util.Iterator;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;

// la etapa que valida que un programa sintácticamente correcto además tenga sentido
// envuelve al parser: consume Result<Statement> y produce Result<Statement>, así se
// encadena sin que el parser ni el intérprete sepan que existe
public final class SemanticAnalyzer implements Iterator<Result<Statement>> {

    private final Iterator<Result<Statement>> statements;
    private final StatementChecker checker;

    public SemanticAnalyzer(Iterator<Result<Statement>> statements, StatementChecker checker) {
        this.statements = statements;
        this.checker = checker;
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    @Override
    public Result<Statement> next() {
        Result<Statement> result = statements.next();
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
