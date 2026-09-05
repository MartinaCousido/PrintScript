package org.printscript.linter.visitors;

import java.util.List;
import java.util.stream.Stream;
import org.printscript.ast.ASTVisitor;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Block;
import org.printscript.ast.BooleanLiteral;
import org.printscript.ast.ConstDeclaration;
import org.printscript.ast.Identifier;
import org.printscript.ast.IfStatement;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.VariableDeclaration;

// junta todos los println de una sentencia, incluidos los que están adentro de un
// bloque. Sin esto, una regla que mire solo la sentencia de nivel superior se saltea
// todos los println de adentro de un if, que es el agujero que tiene el repo relevado.
public final class PrintlnCollector implements ASTVisitor<List<PrintlnStatement>> {

    @Override
    public List<PrintlnStatement> visitPrintlnStatement(PrintlnStatement printlnStatement) {
        return List.of(printlnStatement);
    }

    @Override
    public List<PrintlnStatement> visitBlock(Block block) {
        return block.statements().stream()
                .flatMap(statement -> statement.accept(this).stream())
                .toList();
    }

    @Override
    public List<PrintlnStatement> visitIfStatement(IfStatement ifStatement) {
        return join(
                ifStatement.thenBlock().accept(this),
                ifStatement.elseBranch().map(branch -> branch.accept(this)).orElseGet(List::of));
    }

    @Override
    public List<PrintlnStatement> visitVariableDeclaration(VariableDeclaration declaration) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitConstDeclaration(ConstDeclaration declaration) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitAssignmentStatement(AssignmentStatement assignment) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitNumberLiteral(NumberLiteral numberLiteral) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitStringLiteral(StringLiteral stringLiteral) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitBooleanLiteral(BooleanLiteral booleanLiteral) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitIdentifier(Identifier identifier) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitBinaryExpression(BinaryExpression binaryExpression) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitReadInput(ReadInput readInput) {
        return List.of();
    }

    @Override
    public List<PrintlnStatement> visitReadEnv(ReadEnv readEnv) {
        return List.of();
    }

    private static List<PrintlnStatement> join(
            List<PrintlnStatement> first, List<PrintlnStatement> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
