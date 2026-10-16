package org.printscript.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.printscript.common.ErrorCode;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;

class IterativeTraversalTest {
    private static final String FAILED = "failed";

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 1));
    private static final NumberLiteral ONE = new NumberLiteral(1, RANGE);

    @Test
    void foldPreservesLeftToRightOrderAndStopsBeforeRightEffectsOnFailure() {
        List<String> effects = new ArrayList<>();
        Expression expression =
                new BinaryExpression(
                        new Identifier("left", RANGE),
                        Operator.PLUS,
                        new Identifier("right", RANGE),
                        RANGE);
        Result<String> failure =
                ExpressionFold.evaluate(
                        expression,
                        leaf -> {
                            String name = ((Identifier) leaf).name();
                            effects.add(name);
                            return new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, FAILED, RANGE);
                        },
                        (binary, left, right) -> new Success<>(left + right),
                        (read, argument) -> new Success<>(argument));
        assertEquals(new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, FAILED, RANGE), failure);
        assertEquals(List.of("left"), effects);
    }

    @Test
    void foldHandlesNestedReadsBinaryValuesAndNullLeaves() {
        Expression expression =
                new ReadInput(
                        new ReadEnv(new BinaryExpression(ONE, Operator.PLUS, ONE, RANGE), RANGE),
                        RANGE);
        Result<Integer> result =
                ExpressionFold.evaluate(
                        expression,
                        leaf -> new Success<>(1),
                        (binary, left, right) -> new Success<>(left + right),
                        (read, argument) -> new Success<>(argument + 1));
        assertEquals(new Success<>(4), result);
        assertEquals(
                new Success<>(null),
                ExpressionFold.evaluate(
                        ONE,
                        leaf -> new Success<>(null),
                        (binary, left, right) -> new Success<>(null),
                        (read, argument) -> new Success<>(null)));
    }

    @Test
    void traversalUnwindsScopesAfterFailureAndException() {
        Block nested =
                new Block(
                        List.of(new Block(List.of(new PrintlnStatement(ONE, RANGE)), RANGE)),
                        RANGE);
        AtomicInteger scopes = new AtomicInteger();
        assertEquals(
                new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, FAILED, RANGE),
                StatementTraversal.execute(
                        nested,
                        leaf -> new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, FAILED, RANGE),
                        conditional -> new Success<>(List.of()),
                        scopes::incrementAndGet,
                        scopes::decrementAndGet));
        assertEquals(0, scopes.get());
        assertThrows(
                IllegalStateException.class,
                () ->
                        StatementTraversal.execute(
                                nested,
                                leaf -> {
                                    throw new IllegalStateException(FAILED);
                                },
                                conditional -> new Success<>(List.of()),
                                scopes::incrementAndGet,
                                scopes::decrementAndGet));
        assertEquals(0, scopes.get());
    }

    @Test
    void traversalUsesSelectedBranchesAndPropagatesConditionFailures() {
        Block body = new Block(List.of(new PrintlnStatement(ONE, RANGE)), RANGE);
        IfStatement conditional =
                new IfStatement(new BooleanLiteral(true, RANGE), body, Optional.empty(), RANGE);
        AtomicInteger visited = new AtomicInteger();
        assertEquals(
                new Success<>(null),
                StatementTraversal.execute(
                        conditional,
                        leaf -> {
                            visited.incrementAndGet();
                            return new Success<>(null);
                        },
                        statement -> new Success<>(List.of(statement.thenBlock())),
                        () -> {},
                        () -> {}));
        assertEquals(1, visited.get());
        assertEquals(
                new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "condition", RANGE),
                StatementTraversal.execute(
                        conditional,
                        leaf -> new Success<>(null),
                        statement ->
                                new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "condition", RANGE),
                        () -> {},
                        () -> {}));
    }

    @Test
    void scopedVisitorUsesTheSameTraversalForBlocksAndConditionals() {
        AtomicInteger visited = new AtomicInteger();
        ScopedStatementVisitor visitor =
                new ScopedStatementVisitor() {
                    @Override
                    protected Result<List<Statement>> branches(IfStatement conditional) {
                        return new Success<>(List.of(conditional.thenBlock()));
                    }

                    @Override
                    protected void openScope() {}

                    @Override
                    protected void closeScope() {}

                    @Override
                    public Result<Void> visitPrintlnStatement(PrintlnStatement statement) {
                        visited.incrementAndGet();
                        return new Success<>(null);
                    }

                    @Override
                    public Result<Void> visitVariableDeclaration(VariableDeclaration statement) {
                        return new Success<>(null);
                    }

                    @Override
                    public Result<Void> visitConstDeclaration(ConstDeclaration statement) {
                        return new Success<>(null);
                    }

                    @Override
                    public Result<Void> visitAssignmentStatement(AssignmentStatement statement) {
                        return new Success<>(null);
                    }

                    @Override
                    protected Result<Void> notAStatement(Range range) {
                        return new Failure<>(ErrorCode.UNRECOGNIZED_STATEMENT, "statement", range);
                    }
                };
        Block body = new Block(List.of(new PrintlnStatement(ONE, RANGE)), RANGE);
        assertEquals(new Success<>(null), body.accept(visitor));
        assertEquals(
                new Success<>(null),
                new IfStatement(new BooleanLiteral(true, RANGE), body, Optional.empty(), RANGE)
                        .accept(visitor));
        assertEquals(2, visited.get());
    }
}
