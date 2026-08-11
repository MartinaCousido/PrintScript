package org.printscript.ast;

import org.printscript.common.Range;

public record PrintlnStatement(Expression expression, Range range) implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitPrintlnStatement(this);
    }
}
