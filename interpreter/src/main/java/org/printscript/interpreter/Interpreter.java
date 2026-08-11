package org.printscript.interpreter;

import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Result;
import org.printscript.common.Success;

public final class Interpreter implements PrintScriptIterator<Result<?>> {

    private final PrintScriptIterator<Result<Statement>> statements;
    private final Executor executor;

    public Interpreter(PrintScriptIterator<Result<Statement>> statements, Executor executor) {
        this.statements = statements;
        this.executor = executor;
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    @Override
    public Result<?> getNext() {
        Result<Statement> next = statements.getNext();
        if (next instanceof Failure<Statement>) {
            return next;
        }
        return ((Success<Statement>) next).value().accept(executor);
    }
}
