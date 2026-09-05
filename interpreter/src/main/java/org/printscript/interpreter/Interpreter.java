package org.printscript.interpreter;

import java.util.Iterator;
import java.util.Set;
import org.printscript.ast.Statement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;

// la etapa completa: lo que se enchufa después del parser.
// recie el flujo de sentencias del parser y por cada una llama al Executor
// un error no corta la iteración, se sigue con la siguiente.
// Decidir si frenar o no es una tarea de un módulo de más arriba que no existe
public final class Interpreter implements Iterator<Result<?>> {

    private final Iterator<Result<Statement>> statements;
    private final Executor executor;
    private final Set<Class<? extends Statement>> supportedNodes;

    public Interpreter(Iterator<Result<Statement>> statements, Executor executor) {
        this(statements, executor, null);
    }

    public Interpreter(
            Iterator<Result<Statement>> statements,
            Executor executor,
            Set<Class<? extends Statement>> supportedNodes) {
        this.statements = statements;
        this.executor = executor;
        this.supportedNodes = supportedNodes;
    }

    @Override
    public boolean hasNext() {
        return statements.hasNext();
    }

    @Override
    public Result<?> next() {
        Result<Statement> next = statements.next();
        if (next instanceof Failure<Statement>) {
            return next;
        }
        Statement statement = ((Success<Statement>) next).value();
        if (supportedNodes != null && !supportedNodes.contains(statement.getClass())) {
            return new Failure<>(
                    "Nodo "
                            + statement.getClass().getSimpleName()
                            + " no soportado por PrintScript",
                    statement.range());
        }
        return statement.accept(executor);
    }
}
