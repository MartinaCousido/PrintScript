package org.printscript.ast;

import org.printscript.common.Range;

public record StringLiteral(String value, Range range) implements Expression {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStringLiteral(this);
    }
}
