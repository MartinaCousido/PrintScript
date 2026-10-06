package org.printscript.ast;

import java.util.List;
import org.printscript.common.Result;

/** Shares stack-safe block traversal while each engine decides scopes and branches. */
public abstract class ScopedStatementVisitor extends StatementOnlyVisitor<Result<Void>> {
    @Override
    public final Result<Void> visitBlock(Block block) {
        return traverse(block);
    }

    @Override
    public final Result<Void> visitIfStatement(IfStatement conditional) {
        return traverse(conditional);
    }

    private Result<Void> traverse(Statement statement) {
        return StatementTraversal.execute(
                statement,
                leaf -> leaf.accept(this),
                this::branches,
                this::openScope,
                this::closeScope);
    }

    protected abstract Result<List<Statement>> branches(IfStatement conditional);

    protected abstract void openScope();

    protected abstract void closeScope();
}
