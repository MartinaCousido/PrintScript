package org.printscript.ast;

import org.printscript.common.Range;

// readEnv("NOMBRE"): lee una variable de entorno
// el argumento es obligatorio y tiene que ser un string
public record ReadEnv(Expression name, Range range) implements Expression {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReadEnv(this);
    }
}
