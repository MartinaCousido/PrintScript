package org.printscript.ast;

public interface ASTVisitor<T> {

    T visitNumberLiteral(NumberLiteral numberLiteral);

    T visitStringLiteral(StringLiteral stringLiteral);

    T visitIdentifier(Identifier identifier);

    T visitBinaryExpression(BinaryExpression binaryExpression);

    T visitVariableDeclaration(VariableDeclaration variableDeclaration);

    T visitAssignmentStatement(AssignmentStatement assignmentStatement);

    T visitPrintlnStatement(PrintlnStatement printlnStatement);
}
