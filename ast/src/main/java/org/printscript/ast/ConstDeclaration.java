package org.printscript.ast;

import java.util.Optional;
import org.printscript.common.Range;

// una declaración inmutable: const x: number = 12;
public record ConstDeclaration(
        Identifier name, TypeName type, Optional<Expression> initializer, Range range)
        implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstDeclaration(this);
    }
}
