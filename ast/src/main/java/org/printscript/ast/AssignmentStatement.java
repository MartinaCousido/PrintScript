package org.printscript.ast;

import org.printscript.common.Range;
//una asignación tipo: x= 5; El parser todavia no la arma.
public record AssignmentStatement(String name, Expression expression, Range range) implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignmentStatement(this);
    }
}
