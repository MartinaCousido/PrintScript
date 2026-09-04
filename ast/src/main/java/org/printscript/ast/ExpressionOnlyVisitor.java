package org.printscript.ast;

import org.printscript.common.Range;

// el espejo de StatementOnlyVisitor: para los visitors que solo tienen sentido sobre
// expresiones, como el evaluador y el que calcula tipos
public abstract class ExpressionOnlyVisitor<T> implements ASTVisitor<T> {

    // qué devolver cuando lo que llegó no es una expresión evaluable
    protected abstract T notAnExpression(Range range);

    @Override
    public T visitVariableDeclaration(VariableDeclaration variableDeclaration) {
        return notAnExpression(variableDeclaration.range());
    }

    @Override
    public T visitConstDeclaration(ConstDeclaration constDeclaration) {
        return notAnExpression(constDeclaration.range());
    }

    @Override
    public T visitAssignmentStatement(AssignmentStatement assignmentStatement) {
        return notAnExpression(assignmentStatement.range());
    }

    @Override
    public T visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return notAnExpression(printlnStatement.range());
    }
}
