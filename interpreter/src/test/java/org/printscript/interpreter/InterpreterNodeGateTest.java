package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.PrintlnStatement;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;

class InterpreterNodeGateTest {

    private static final Range ANY = new Range(new Position(1, 1), new Position(1, 1));

    private static final class InMemoryPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String line) {
            lines.add(line);
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

    private static Interpreter gatedInterpreter(OutputPrinter printer, List<Result<Statement>> results) {
        return new Interpreter(
            new ResultStream(results),
            new Executor(new Environment(), printer),
            Set.of(PrintlnStatement.class),
            "1.0");
    }

    @Test
    void aNodeOutsideTheSupportedSetProducesAFailureWithRangeAndMessageWithoutExecuting() {
        Range range = new Range(new Position(3, 5), new Position(3, 20));
        Statement declaration = new VariableDeclaration(
            "x",
            new TypeName("number", ANY),
            Optional.of(new NumberLiteral(1, ANY)),
            range);
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter = gatedInterpreter(
            printer,
            List.of(
                new Success<>(declaration),
                new Success<>(new PrintlnStatement(new Identifier("x", ANY), ANY))));

        Result<?> first = interpreter.getNext();
        Result<?> second = interpreter.getNext();

        Failure<?> failure = assertInstanceOf(Failure.class, first, String.valueOf(first));
        assertEquals(range, failure.range());
        assertTrue(failure.message().contains("VariableDeclaration"), failure.message());
        assertTrue(failure.message().contains("1.0"), failure.message());
        assertInstanceOf(Failure.class, second, String.valueOf(second));
        assertEquals(List.of(), printer.lines);
    }

    @Test
    void aNodeInsideTheSupportedSetIsExecuted() {
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter = gatedInterpreter(
            printer,
            List.of(new Success<>(new PrintlnStatement(new StringLiteral("hola", ANY), ANY))));

        Result<?> result = interpreter.getNext();

        assertInstanceOf(Success.class, result, String.valueOf(result));
        assertEquals(List.of("hola"), printer.lines);
    }
}
