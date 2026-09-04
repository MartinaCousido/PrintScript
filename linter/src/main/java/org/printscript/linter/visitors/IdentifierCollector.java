package org.printscript.linter.visitors;

import java.util.List;
import java.util.stream.Stream;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;

public final class IdentifierCollector implements ASTVisitor<List<Identifier>> {

    @Override
    public List<Identifier> visitNumberLiteral(NumberLiteral numberLiteral) {
        return List.of();
    }

    @Override
    public List<Identifier> visitStringLiteral(StringLiteral stringLiteral) {
        return List.of();
    }

    @Override
    public List<Identifier> visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return List.of();
    }

    @Override
    public List<Identifier> visitIdentifier(Identifier identifier) {
        return List.of(identifier);
    }

    @Override
    public List<Identifier> visitBinaryExpression(BinaryExpression binaryExpression) {
        return join(in(binaryExpression.left()), in(binaryExpression.right()));
    }

    @Override
    public List<Identifier> visitVariableDeclaration(VariableDeclaration variableDeclaration) {
        return join(
                List.of(variableDeclaration.name()),
                variableDeclaration.initializer().map(this::in).orElseGet(List::of));
    }

    @Override
    public List<Identifier> visitAssignmentStatement(AssignmentStatement assignmentStatement) {
        return join(List.of(assignmentStatement.name()), in(assignmentStatement.expression()));
    }

    @Override
    public List<Identifier> visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return in(printlnStatement.expression());
    }

    private List<Identifier> in(Expression expression) {
        return expression.accept(this);
    }

    private static List<Identifier> join(List<Identifier> first, List<Identifier> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
