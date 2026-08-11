package org.printscript.ast;

import java.util.Optional;
import org.printscript.common.Range;
//una declaración: let x: number = 12; guarda el nombre, el tipo y el valor inicial
public record VariableDeclaration(String name, TypeName type, Optional<Expression> initializer, Range range) implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariableDeclaration(this);
    }
}
