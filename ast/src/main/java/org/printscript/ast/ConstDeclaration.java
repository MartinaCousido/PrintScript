package org.printscript.ast;

import java.util.Optional;
import org.printscript.common.Range;

// una declaración inmutable: const x: number = 12;
// es un nodo propio y no un flag adentro de VariableDeclaration a propósito: así el
// visitor exhaustivo obliga a cada herramienta a decidir qué hace con una constante,
// en vez de que alguna se olvide de mirar el flag y nadie se entere
// el inicializador es opcional en el árbol aunque una constante sin valor no tenga
// sentido: eso lo rechaza semantic, para que la gramática de las dos declaraciones
// siga siendo la misma
public record ConstDeclaration(
        Identifier name, TypeName type, Optional<Expression> initializer, Range range)
        implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitConstDeclaration(this);
    }
}
