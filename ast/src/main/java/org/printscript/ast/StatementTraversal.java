package org.printscript.ast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;

/** Visits nested statements and unwinds every entered scope on failure or exception. */
public final class StatementTraversal {
    private StatementTraversal() {}

    public static Result<Void> execute(
            Statement root,
            Function<Statement, Result<Void>> leaf,
            Function<IfStatement, Result<List<Statement>>> branches,
            Runnable openScope,
            Runnable closeScope) {
        Deque<Task> pending = new ArrayDeque<>();
        pending.push(new Task(root));
        int openScopes = 0;
        try {
            while (!pending.isEmpty()) {
                Statement statement = pending.pop().statement();
                if (statement == null) {
                    closeScope.run();
                    openScopes--;
                } else if (statement instanceof Block block) {
                    openScope.run();
                    openScopes++;
                    pending.push(new Task(null));
                    enqueue(pending, block.statements());
                } else if (statement instanceof IfStatement conditional) {
                    Result<List<Statement>> selected = branches.apply(conditional);
                    if (selected instanceof Failure<List<Statement>> failure) {
                        return new Failure<>(failure.message(), failure.range());
                    }
                    enqueue(pending, ((Success<List<Statement>>) selected).value());
                } else {
                    Result<Void> result = leaf.apply(statement);
                    if (result instanceof Failure<Void>) {
                        return result;
                    }
                }
            }
            return new Success<>(null);
        } finally {
            while (openScopes > 0) {
                closeScope.run();
                openScopes--;
            }
        }
    }

    private static void enqueue(Deque<Task> pending, List<Statement> statements) {
        for (int index = statements.size() - 1; index >= 0; index--) {
            pending.push(new Task(statements.get(index)));
        }
    }

    private record Task(Statement statement) {}
}
