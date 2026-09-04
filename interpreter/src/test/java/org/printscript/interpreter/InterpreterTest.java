package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.AssignmentStatement;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;

class InterpreterTest {

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

    private static final class ResultStream implements PrintScriptIterator<Result<Statement>> {

        private final List<Result<Statement>> results;
        private int index;

        private ResultStream(List<Result<Statement>> results) {
            this.results = results;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < results.size();
        }

        @Override
        public Result<Statement> getNext() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Result<Statement> result = results.get(index);
            index = index + 1;
            return result;
        }
    }

    private static Range rangeAt(int startLine, int startColumn, int endLine, int endColumn) {
        return new Range(new Position(startLine, startColumn), new Position(endLine, endColumn));
    }

    private static Result<Statement> declaration(String name, double value) {
        Statement statement =
                new VariableDeclaration(
                        new Identifier(name, ANY),
                        new TypeName("number", ANY),
                        Optional.of(new NumberLiteral(value, ANY)),
                        ANY);
        return new Success<>(statement);
    }

    private static Result<Statement> assignment(String name, double value, Range range) {
        return new Success<>(
                new AssignmentStatement(
                        new Identifier(name, ANY), new NumberLiteral(value, ANY), range));
    }

    private static Result<Statement> println(String name) {
        return new Success<>(new PrintlnStatement(new Identifier(name, ANY), ANY));
    }

    @Test
    void aStreamOfValidStatementsIsExecutedInOrder() {
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter =
                interpreterOf(
                        printer,
                        List.of(
                                declaration("x", 1),
                                println("x"),
                                assignment("x", 2, ANY),
                                println("x")));

        List<Result<?>> results = drain(interpreter);

        assertEquals(List.of("1", "2"), printer.lines);
        results.forEach(result -> assertInstanceOf(Success.class, result, String.valueOf(result)));
    }

    @Test
    void aFailureFromAPreviousStageIsPassedThroughWithoutExecuting() {
        Range range = rangeAt(7, 3, 7, 9);
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter =
                interpreterOf(printer, List.of(new Failure<>("falta un punto y coma", range)));

        Result<?> result = interpreter.getNext();

        Failure<?> failure = assertInstanceOf(Failure.class, result, String.valueOf(result));
        assertEquals("falta un punto y coma", failure.message());
        assertEquals(range, failure.range());
        assertEquals(List.of(), printer.lines);
    }

    @Test
    void aStatementThatFailsAtRuntimeDoesNotPreventTheNextOne() {
        Range range = rangeAt(4, 1, 4, 6);
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter =
                interpreterOf(
                        printer,
                        List.of(assignment("nunca", 9, range), declaration("y", 7), println("y")));

        List<Result<?>> results = drain(interpreter);

        assertInstanceOf(Failure.class, results.get(0), String.valueOf(results.get(0)));
        assertEquals(range, ((Failure<?>) results.get(0)).range());
        assertEquals(List.of("7"), printer.lines);
    }

    private static Interpreter interpreterOf(
            OutputPrinter printer, List<Result<Statement>> results) {
        return new Interpreter(new ResultStream(results), new Executor(new Environment(), printer));
    }

    private static List<Result<?>> drain(Interpreter interpreter) {
        List<Result<?>> results = new ArrayList<>();
        while (interpreter.hasNext()) {
            results.add(interpreter.getNext());
        }
        assertFalse(interpreter.hasNext());
        return results;
    }
}
