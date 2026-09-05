package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

class ReadEndToEndTest {

    // acumula todo lo escrito tal cual, para poder ver si el prompt saltó de línea o no
    private static final class RawPrinter implements OutputPrinter {

        // se guarda cada escritura tal cual, sin suponer dónde termina una línea: es lo
        // que permite ver si el prompt saltó de línea o no
        private final List<String> written = new ArrayList<>();

        @Override
        public void println(String line) {
            written.add(line + "\n");
        }

        @Override
        public void print(String text) {
            written.add(text);
        }

        private String raw() {
            return String.join("", written);
        }

        private List<String> lines() {
            List<String> lines = new ArrayList<>(List.of(raw().split("\n", -1)));
            lines.removeLast();
            return lines;
        }
    }

    private static RuntimePorts ports(
            RawPrinter printer, List<String> input, Map<String, String> environment) {
        Deque<String> pending = new ArrayDeque<>(input);
        return new RuntimePorts(
                printer,
                () -> pending.isEmpty() ? Optional.empty() : Optional.of(pending.removeFirst()),
                name -> Optional.ofNullable(environment.get(name)));
    }

    private static RawPrinter execute(List<String> input, String... lines) {
        RawPrinter printer = new RawPrinter();
        new ExecutionRunner()
                .run(List.of(lines).iterator(), ports(printer, input, Map.of()), Version.V11);
        return printer;
    }

    private static List<Failure<?>> validate(String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), Version.V11);
    }

    // --- readInput ---

    @Test
    void aStringReadIsUsedAsIs() {
        RawPrinter printer =
                execute(List.of("Joe"), "let n: string = readInput(\"nombre: \");", "println(n);");

        assertEquals(List.of("nombre: Joe"), printer.lines());
    }

    @Test
    void thePromptDoesNotBreakTheLine() {
        // lo tipeado por el usuario va en la misma línea que el prompt
        RawPrinter printer = execute(List.of("x"), "let n: string = readInput(\"dame: \");");

        assertTrue(printer.raw().startsWith("dame: "));
        assertFalse(printer.raw().startsWith("dame: \n"));
    }

    @Test
    void theDeclaredTypeDrivesTheConversion() {
        RawPrinter printer =
                execute(
                        List.of("42", "true"),
                        "let n: number = readInput(\"n: \");",
                        "let b: boolean = readInput(\"b: \");",
                        "println(n + 1);",
                        "println(b);");

        assertEquals(List.of("n: b: 43", "true"), printer.lines());
    }

    @Test
    void aConversionThatFailsIsAnErrorAndNotAnException() {
        RawPrinter printer = new RawPrinter();

        List<Failure<?>> failures =
                new ExecutionRunner()
                        .run(
                                List.of("let n: number = readInput(\"n: \");").iterator(),
                                ports(printer, List.of("abc"), Map.of()),
                                Version.V11);

        assertEquals(1, failures.size());
        assertEquals("'abc' no es un number", failures.getFirst().message());
    }

    @Test
    void booleanConversionIsStrict() {
        assertEquals(
                "'TRUE' no es un boolean",
                new ExecutionRunner()
                        .run(
                                List.of("let b: boolean = readInput(\"b: \");").iterator(),
                                ports(new RawPrinter(), List.of("TRUE"), Map.of()),
                                Version.V11)
                        .getFirst()
                        .message());
    }

    @Test
    void runningOutOfInputIsReportedAsAnError() {
        List<Failure<?>> failures =
                new ExecutionRunner()
                        .run(
                                List.of("let n: string = readInput(\"n: \");").iterator(),
                                ports(new RawPrinter(), List.of(), Map.of()),
                                Version.V11);

        assertEquals("No hay más entrada disponible para readInput", failures.getFirst().message());
    }

    // --- readEnv ---

    @Test
    void readEnvGoesThroughThePortAndNotThroughTheRealEnvironment() {
        RawPrinter printer = new RawPrinter();

        new ExecutionRunner()
                .run(
                        List.of("let home: string = readEnv(\"MI_VARIABLE\");", "println(home);")
                                .iterator(),
                        ports(printer, List.of(), Map.of("MI_VARIABLE", "un valor")),
                        Version.V11);

        assertEquals(List.of("un valor"), printer.lines());
    }

    @Test
    void readEnvAlsoConvertsToTheDeclaredType() {
        RawPrinter printer = new RawPrinter();

        new ExecutionRunner()
                .run(
                        List.of("let port: number = readEnv(\"PORT\");", "println(port + 1);")
                                .iterator(),
                        ports(printer, List.of(), Map.of("PORT", "8080")),
                        Version.V11);

        assertEquals(List.of("8081"), printer.lines());
    }

    @Test
    void aMissingEnvironmentVariableIsAnError() {
        List<Failure<?>> failures =
                new ExecutionRunner()
                        .run(
                                List.of("let x: string = readEnv(\"NO_EXISTE\");").iterator(),
                                ports(new RawPrinter(), List.of(), Map.of()),
                                Version.V11);

        assertEquals(
                "La variable de entorno NO_EXISTE no está definida", failures.getFirst().message());
    }

    // --- las reglas semánticas ---

    @Test
    void aReadIsCompatibleWithAnyDeclaredType() {
        assertEquals(List.of(), validate("let n: number = readInput(\"n: \");"));
        assertEquals(List.of(), validate("let s: string = readInput(\"s: \");"));
        assertEquals(List.of(), validate("let b: boolean = readEnv(\"B\");"));
    }

    @Test
    void theExceptionDoesNotReachInsideAnExpression() {
        // adentro de una expresión es un string común, y string + number da string
        List<Failure<?>> failures = validate("let n: number = readInput(\"n: \") + 1;");

        assertEquals(1, failures.size());
        assertEquals(
                "No se puede asignar string a una variable number", failures.getFirst().message());
    }

    @Test
    void theArgumentHasToBeAString() {
        assertEquals(
                "El argumento de readInput tiene que ser string",
                validate("let n: string = readInput(5);").getFirst().message());
        assertEquals(
                "El argumento de readEnv tiene que ser string",
                validate("let n: string = readEnv(true);").getFirst().message());
    }

    @Test
    void theArgumentIsMandatory() {
        assertFalse(validate("let n: string = readInput();").isEmpty());
    }

    @Test
    void version10HasNeitherFunction() {
        assertFalse(
                new ValidationRunner()
                        .validate(
                                List.of("let n: string = readInput(\"n: \");").iterator(),
                                Version.V10)
                        .isEmpty());
    }

    @Test
    void aReadCanBeAssignedToAnAlreadyDeclaredVariable() {
        RawPrinter printer =
                execute(
                        List.of("7"),
                        "let n: number = 0;",
                        "n = readInput(\"n: \");",
                        "println(n);");

        assertEquals(List.of("n: 7"), printer.lines());
    }
}
