package org.printscript.ast;

import java.util.List;
import org.printscript.common.Range;

// las sentencias entre llaves de un if
// es un nodo y no una List<Statement> pelada por dos razones. La primera es que el
// formatter tiene que poder distinguir "else if (b) {}" de "else { if (b) {} }": el
// primero lleva un IfStatement pelado en la rama else y el segundo un Block que lo
// contiene, y sin el nodo los dos árboles serían iguales. La segunda es que el scope
// pasa a ser exactamente un Block, así que semantic y el intérprete abren y cierran
// frame en un solo lugar, sin casos especiales para then y para else.
// Lo construye el parser del if: ningún StatementParser matchea una '{' suelta, así
// que un bloque huérfano nunca se arma y no hay que agregar un BlockParser.
public record Block(List<Statement> statements, Range range) implements Statement {

    public Block {
        statements = List.copyOf(statements);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
