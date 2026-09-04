package org.printscript.ast;

import org.printscript.common.Range;

// readInput("mensaje"): pide una línea al usuario mostrando antes ese mensaje
// el argumento es obligatorio y tiene que ser un string
public record ReadInput(Expression prompt, Range range) implements Expression {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReadInput(this);
    }
}
