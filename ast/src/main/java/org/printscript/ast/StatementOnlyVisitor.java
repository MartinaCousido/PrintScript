package org.printscript.ast;

import org.printscript.common.Range;

// para los visitors que solo tienen sentido sobre sentencias: el ejecutor y el
// chequeador semántico. Rechazan toda expresión de la misma manera, y cada nodo nuevo
// de expresión se agrega una sola vez acá en lugar de en cada implementación.
public abstract class StatementOnlyVisitor<T> implements ASTVisitor<T> {

    // qué devolver cuando lo que llegó no es una sentencia ejecutable
    protected abstract T notAStatement(Range range);

    @Override
    public T visitNumberLiteral(NumberLiteral numberLiteral) {
        return notAStatement(numberLiteral.range());
    }

    @Override
    public T visitStringLiteral(StringLiteral stringLiteral) {
        return notAStatement(stringLiteral.range());
    }

    @Override
    public T visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return notAStatement(booleanLiteral.range());
    }

    @Override
    public T visitIdentifier(Identifier identifier) {
        return notAStatement(identifier.range());
    }

    @Override
    public T visitBinaryExpression(BinaryExpression binaryExpression) {
        return notAStatement(binaryExpression.range());
    }
}
