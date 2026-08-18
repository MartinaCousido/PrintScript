package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.printscript.ast.PrintlnStatement;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.interpreter.Environment;
import org.printscript.interpreter.Executor;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.providers.InterpreterVersionProvider;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.providers.LexerVersionProvider;
import org.printscript.parser.Parser;
import org.printscript.parser.ParserVersionProvider;
import org.printscript.parser.TokenBuffer;
import org.printscript.runtime.OutputPrinter;

class ProviderConsistencyTest {

    private static final String VERSION = "1.0";
    private static final String UNKNOWN_VERSION = "9.9";
    private static final String UNKNOWN_VERSION_MESSAGE =
        "Versión de PrintScript desconocida: 9.9. Versiones soportadas: 1.0";

    private static final class InMemoryPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String line) {
            lines.add(line);
        }
    }

    private static Parser parserFromProviders(String... lines) {
        Lexer lexer = new LexerVersionProvider().providerFor(VERSION).provide(List.of(lines).iterator());
        TokenBuffer buffer = new TokenBuffer(lexer);
        return new ParserVersionProvider().providerFor(VERSION).provide(buffer);
    }

    private static List<Result<?>> drain(Interpreter interpreter) {
        List<Result<?>> results = new ArrayList<>();
        while (interpreter.hasNext()) {
            results.add(interpreter.getNext());
        }
        return results;
    }

    private static List<Failure<?>> failuresOf(List<Result<?>> results) {
        List<Failure<?>> failures = new ArrayList<>();
        for (Result<?> result : results) {
            if (result instanceof Failure<?> failure) {
                failures.add(failure);
            }
        }
        return failures;
    }

    private static List<Failure<?>> runWithProviders(InMemoryPrinter printer, String... lines) {
        Parser parser = parserFromProviders(lines);
        Interpreter interpreter = new InterpreterVersionProvider().providerFor(VERSION).provide(parser, printer);
        return failuresOf(drain(interpreter));
    }

    @Test
    void stringDeclarationAndConcatenationExampleRunsWithTheThreeProvidersOfVersion10() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runWithProviders(
            printer,
            "let name: string = \"Joe\";",
            "let lastName: string = \"Doe\";",
            "println(name + \" \" + lastName);");

        assertEquals(List.of("Joe Doe"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void numberArithmeticWithStringConcatenationExampleRunsWithTheThreeProvidersOfVersion10() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runWithProviders(
            printer,
            "let a: number = 12;",
            "let b: number = 4;",
            "let c: number = a / b;",
            "println(\"Result: \" + c);");

        assertEquals(List.of("Result: 3"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void assignmentExampleRunsWithTheThreeProvidersOfVersion10() {
        InMemoryPrinter printer = new InMemoryPrinter();

        List<Failure<?>> failures = runWithProviders(
            printer,
            "let a: number = 12;",
            "let b: number = 4;",
            "a = a / b;",
            "println(\"Result: \" + a);");

        assertEquals(List.of("Result: 3"), printer.lines);
        assertEquals(List.of(), failures);
    }

    @Test
    void anUnknownVersionFailsInTheThreeStagesWithTheSameExceptionTypeAndMessage() {
        IllegalArgumentException lexerError = assertThrows(
            IllegalArgumentException.class,
            () -> new LexerVersionProvider().providerFor(UNKNOWN_VERSION));
        IllegalArgumentException parserError = assertThrows(
            IllegalArgumentException.class,
            () -> new ParserVersionProvider().providerFor(UNKNOWN_VERSION));
        IllegalArgumentException interpreterError = assertThrows(
            IllegalArgumentException.class,
            () -> new InterpreterVersionProvider().providerFor(UNKNOWN_VERSION));

        assertEquals(UNKNOWN_VERSION_MESSAGE, lexerError.getMessage());
        assertEquals(lexerError.getMessage(), parserError.getMessage());
        assertEquals(parserError.getMessage(), interpreterError.getMessage());
    }

    @Test
    void aStatementOutsideTheSupportedNodeSetProducesAnExplicitFailureAndNoOutput() {
        InMemoryPrinter printer = new InMemoryPrinter();
        Parser parser = parserFromProviders(
            "let a: number = 5;",
            "println(a);");
        Interpreter interpreter = new Interpreter(
            parser,
            new Executor(new Environment(), printer),
            Set.of(PrintlnStatement.class),
            VERSION);

        List<Result<?>> results = drain(interpreter);

        assertEquals(2, results.size());
        Failure<?> gateFailure = assertInstanceOf(Failure.class, results.getFirst(), String.valueOf(results.getFirst()));
        assertEquals("Nodo VariableDeclaration no soportado por la versión 1.0 de PrintScript", gateFailure.message());
        assertInstanceOf(Failure.class, results.get(1), String.valueOf(results.get(1)));
        assertEquals(List.of(), printer.lines);
    }
}
