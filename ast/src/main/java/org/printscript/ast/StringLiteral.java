package org.printscript.ast;

import org.printscript.common.Range;

// un texto escrito directo en el codigo, ya sin las comillas
public record StringLiteral(String value, Range range) implements Expression {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitStringLiteral(this);
    }
}
