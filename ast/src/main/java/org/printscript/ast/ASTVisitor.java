package org.printscript.ast;

// un metodo por cada tipo de nodo. Quien quiera recorrer el árbol y hacer algo con él, implementa
// esta interfaz
// si se agrega un nodo nuevo, deja de compilar tod lo que la implementa
public interface ASTVisitor<T> {

    T visitNumberLiteral(NumberLiteral numberLiteral);

    T visitStringLiteral(StringLiteral stringLiteral);

    T visitBooleanLiteral(BooleanLiteral booleanLiteral);

    T visitIdentifier(Identifier identifier);

    T visitBinaryExpression(BinaryExpression binaryExpression);

    T visitReadInput(ReadInput readInput);

    T visitReadEnv(ReadEnv readEnv);

    T visitVariableDeclaration(VariableDeclaration variableDeclaration);

    T visitConstDeclaration(ConstDeclaration constDeclaration);

    T visitAssignmentStatement(AssignmentStatement assignmentStatement);

    T visitPrintlnStatement(PrintlnStatement printlnStatement);

    T visitBlock(Block block);

    T visitIfStatement(IfStatement ifStatement);
}
