package org.printscript.ast;

import org.printscript.common.Range;

// true o false escrito directo en el codigo
public record BooleanLiteral(boolean value, Range range) implements Expression {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBooleanLiteral(this);
    }
}
