package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.printscript.common.Position;
import org.printscript.common.PrintScriptIterator;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.interpreter.providers.InterpreterProvider;
import org.printscript.interpreter.providers.InterpreterVersionProvider;
import org.printscript.runtime.OutputPrinter;

class InterpreterVersionProviderTest {

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

    private static Result<Statement> declaration(String name, double value) {
        Statement statement = new VariableDeclaration(
            name,
            new TypeName("number", ANY),
            Optional.of(new NumberLiteral(value, ANY)),
            ANY);
        return new Success<>(statement);
    }

    private static Result<Statement> assignment(String name, double value) {
        return new Success<>(new AssignmentStatement(name, new NumberLiteral(value, ANY), ANY));
    }

    private static Result<Statement> println(String name) {
        return new Success<>(new PrintlnStatement(new Identifier(name, ANY), ANY));
    }

    @Test
    void version10ProvidesAnInterpreterThatExecutesTheThreeStatements() {
        InterpreterProvider provider = InterpreterVersionProvider.providerFor("1.0");
        InMemoryPrinter printer = new InMemoryPrinter();
        Interpreter interpreter = provider.provide(
            new ResultStream(List.of(
                declaration("x", 1),
                println("x"),
                assignment("x", 2),
                println("x"))),
            printer);

        List<Result<?>> results = new ArrayList<>();
        while (interpreter.hasNext()) {
            results.add(interpreter.getNext());
        }

        assertEquals(List.of("1", "2"), printer.lines);
        results.forEach(result -> assertInstanceOf(Success.class, result, String.valueOf(result)));
    }

    @Test
    void anUnknownVersionFailsWithAClearMessage() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> InterpreterVersionProvider.providerFor("9.9"));

        assertTrue(error.getMessage().contains("9.9"), error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}
