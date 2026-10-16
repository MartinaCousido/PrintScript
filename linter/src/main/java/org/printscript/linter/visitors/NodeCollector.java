package org.printscript.linter.visitors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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

/** One preorder traversal for all collectors; nodes are accumulated exactly once. */
abstract class NodeCollector<T> implements ASTVisitor<List<T>> {
    private final Class<T> selectedType;

    NodeCollector(Class<T> selectedType) {
        this.selectedType = selectedType;
    }

    private List<T> collect(Object root) {
        List<T> collected = new ArrayList<>();
        Deque<Object> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            Object node = pending.pop();
            if (selectedType.isInstance(node)) {
                collected.add(selectedType.cast(node));
            }
            enqueueChildren(pending, node);
        }
        return List.copyOf(collected);
    }

    private static void enqueueChildren(Deque<Object> pending, Object node) {
        switch (node) {
            case BinaryExpression binary -> {
                pending.push(binary.right());
                pending.push(binary.left());
            }
            case VariableDeclaration declaration -> {
                declaration.initializer().ifPresent(pending::push);
                pending.push(declaration.name());
            }
            case ConstDeclaration declaration -> {
                declaration.initializer().ifPresent(pending::push);
                pending.push(declaration.name());
            }
            case AssignmentStatement assignment -> {
                pending.push(assignment.expression());
                pending.push(assignment.name());
            }
            case PrintlnStatement println -> pending.push(println.expression());
            case Block block -> {
                for (int index = block.statements().size() - 1; index >= 0; index--) {
                    pending.push(block.statements().get(index));
                }
            }
            case IfStatement conditional -> {
                conditional.elseBranch().ifPresent(pending::push);
                pending.push(conditional.thenBlock());
                pending.push(conditional.condition());
            }
            case ReadInput input -> pending.push(input.prompt());
            case ReadEnv environment -> pending.push(environment.name());
            case NumberLiteral _, StringLiteral _, BooleanLiteral _, Identifier _ -> {}
            default -> throw new IllegalArgumentException("Unknown AST node");
        }
    }

    @Override
    public final List<T> visitNumberLiteral(NumberLiteral node) {
        return collect(node);
    }

    @Override
    public final List<T> visitStringLiteral(StringLiteral node) {
        return collect(node);
    }

    @Override
    public final List<T> visitBooleanLiteral(BooleanLiteral node) {
        return collect(node);
    }

    @Override
    public final List<T> visitIdentifier(Identifier node) {
        return collect(node);
    }

    @Override
    public final List<T> visitBinaryExpression(BinaryExpression node) {
        return collect(node);
    }

    @Override
    public final List<T> visitVariableDeclaration(VariableDeclaration node) {
        return collect(node);
    }

    @Override
    public final List<T> visitConstDeclaration(ConstDeclaration node) {
        return collect(node);
    }

    @Override
    public final List<T> visitAssignmentStatement(AssignmentStatement node) {
        return collect(node);
    }

    @Override
    public final List<T> visitPrintlnStatement(PrintlnStatement node) {
        return collect(node);
    }

    @Override
    public final List<T> visitBlock(Block node) {
        return collect(node);
    }

    @Override
    public final List<T> visitIfStatement(IfStatement node) {
        return collect(node);
    }

    @Override
    public final List<T> visitReadInput(ReadInput node) {
        return collect(node);
    }

    @Override
    public final List<T> visitReadEnv(ReadEnv node) {
        return collect(node);
    }
}
