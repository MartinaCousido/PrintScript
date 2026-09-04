package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.BinaryExpression;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.Operator;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.runtime.OutputPrinter;

class ExecutorTest {

    private static final Range ANY = rangeAt(1, 1, 1, 1);

    private static final class InMemoryPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void println(String line) {
            lines.add(line);
        }

        @Override
        public void print(String text) {
            lines.add(text);
        }
    }

    private static Range rangeAt(int startLine, int startColumn, int endLine, int endColumn) {
        return new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));
    }

    private static NumberLiteral number(double value) {
        return new NumberLiteral(value, ANY);
    }

    private static Statement declaration(String name, Expression initializer, Range range) {
        return new VariableDeclaration(
                new Identifier(name, ANY),
                new TypeName("number", ANY),
                Optional.of(initializer),
                range);
    }

    private static Result<Void> run(Executor executor, Statement statement) {
        return statement.accept(executor);
    }

    private static Failure<Void> runFailing(Executor executor, Statement statement) {
        Result<Void> result = statement.accept(executor);
        return assertInstanceOf(Failure.class, result, String.valueOf(result));
    }

    private static List<String> printedBy(Statement... statements) {
        InMemoryPrinter printer = new InMemoryPrinter();
        Executor executor = new Executor(new Environment(), Ports.of(printer));
        for (Statement statement : statements) {
            run(executor, statement);
        }
        return printer.lines;
    }

    @Test
    void theThreeStatementsOfVersion10AreExecuted() {
        List<String> printed =
                printedBy(
                        declaration("x", number(1), ANY),
                        new AssignmentStatement(new Identifier("x", ANY), number(5), ANY),
                        new PrintlnStatement(new Identifier("x", ANY), ANY));

        assertEquals(List.of("5"), printed);
    }

    @Test
    void aNumberWithoutDecimalPartIsPrintedAsAnInteger() {
        Expression division = new BinaryExpression(number(12), Operator.DIVIDE, number(4), ANY);

        assertEquals(List.of("3"), printedBy(new PrintlnStatement(division, ANY)));
    }

    @Test
    void aRealDecimalKeepsItsDecimals() {
        Expression division = new BinaryExpression(number(13), Operator.DIVIDE, number(4), ANY);

        assertEquals(List.of("3.25"), printedBy(new PrintlnStatement(division, ANY)));
    }

    @Test
    void redeclaringAVariableFailsWithTheRangeOfTheStatement() {
        Range second = rangeAt(2, 1, 2, 18);
        Executor executor = new Executor(new Environment(), Ports.of(new InMemoryPrinter()));
        run(executor, declaration("x", number(1), ANY));

        Failure<Void> failure = runFailing(executor, declaration("x", number(2), second));

        assertEquals(second, failure.range());
        assertTrue(failure.message().contains("x"), failure.message());
    }

    @Test
    void assigningToAnUndeclaredVariableFailsWithTheRangeOfTheStatement() {
        Range range = rangeAt(3, 1, 3, 7);
        Executor executor = new Executor(new Environment(), Ports.of(new InMemoryPrinter()));

        Failure<Void> failure =
                runFailing(
                        executor,
                        new AssignmentStatement(new Identifier("y", ANY), number(1), range));

        assertEquals(range, failure.range());
        assertTrue(failure.message().contains("y"), failure.message());
    }
}
