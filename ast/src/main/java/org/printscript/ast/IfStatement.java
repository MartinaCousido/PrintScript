package org.printscript.ast;

import java.util.Optional;
import org.printscript.common.Range;

// if (condicion) { ... } con su else opcional
// no existe ningún nodo ElseIf: la rama else es una sentencia, que puede ser un Block
// o directamente otro IfStatement. Eso es todo lo que hace falta para "else if", y la
// recursión la resuelve el parser.
public record IfStatement(
        Expression condition, Block thenBlock, Optional<Statement> elseBranch, Range range)
        implements Statement {

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIfStatement(this);
    }
}
