package org.printscript.ast;

import java.util.List;
import org.printscript.common.Range;

// las sentencias entre llaves de un if
public record Block(List<Statement> statements, Range range) implements Statement {

    public Block {
        statements = List.copyOf(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
