package org.printscript.linter.visitors;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;

// junta todos los readInput de una sentencia.
public final class ReadInputCollector implements ASTVisitor<List<ReadInput>> {

    @Override
    public List<ReadInput> visitReadInput(ReadInput readInput) {
        return List.of(readInput);
    }

    @Override
    public List<ReadInput> visitBinaryExpression(BinaryExpression binaryExpression) {
        return join(binaryExpression.left().accept(this), binaryExpression.right().accept(this));
    }

    @Override
    public List<ReadInput> visitVariableDeclaration(VariableDeclaration declaration) {
        return inside(declaration.initializer());
    }

    @Override
    public List<ReadInput> visitConstDeclaration(ConstDeclaration declaration) {
        return inside(declaration.initializer());
    }

    @Override
    public List<ReadInput> visitAssignmentStatement(AssignmentStatement assignment) {
        return assignment.expression().accept(this);
    }

    @Override
    public List<ReadInput> visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return printlnStatement.expression().accept(this);
    }

    @Override
    public List<ReadInput> visitBlock(Block block) {
        return block.statements().stream().flatMap(this::within).toList();
    }

    @Override
    public List<ReadInput> visitIfStatement(IfStatement ifStatement) {
        return join(
                join(ifStatement.condition().accept(this), ifStatement.thenBlock().accept(this)),
                ifStatement.elseBranch().map(branch -> branch.accept(this)).orElseGet(List::of));
    }

    @Override
    public List<ReadInput> visitReadEnv(ReadEnv readEnv) {
        return readEnv.name().accept(this);
    }

    @Override
    public List<ReadInput> visitNumberLiteral(NumberLiteral numberLiteral) {
        return List.of();
    }

    @Override
    public List<ReadInput> visitStringLiteral(StringLiteral stringLiteral) {
        return List.of();
    }

    @Override
    public List<ReadInput> visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return List.of();
    }

    @Override
    public List<ReadInput> visitIdentifier(Identifier identifier) {
        return List.of();
    }

    private Stream<ReadInput> within(Statement statement) {
        return statement.accept(this).stream();
    }

    private List<ReadInput> inside(Optional<Expression> expression) {
        return expression.map(inner -> inner.accept(this)).orElseGet(List::of);
    }

    private static List<ReadInput> join(List<ReadInput> first, List<ReadInput> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
