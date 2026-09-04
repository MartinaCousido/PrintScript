package org.printscript.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.ast.Expression;
import org.printscript.ast.Identifier;
import org.printscript.ast.NumberLiteral;
import org.printscript.ast.ReadEnv;
import org.printscript.ast.ReadInput;
import org.printscript.ast.Statement;
import org.printscript.ast.StringLiteral;
import org.printscript.ast.TypeName;
import org.printscript.ast.VariableDeclaration;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;

class ReadsTest {

    private static final Range RANGE = new Range(new Position(1, 1), new Position(1, 4));

    private final List<String> written = new ArrayList<>();

    private final OutputPrinter printer =
            new OutputPrinter() {

                @Override
                public void println(String line) {
                    written.add(line + "\n");
                }

                @Override
                public void print(String text) {
                    written.add(text);
                }
            };

    private RuntimePorts ports(List<String> input, Map<String, String> environment) {
        return Ports.of(printer, input, environment);
    }

    private Result<Object> evaluate(Expression expression, RuntimePorts ports) {
        return expression.accept(new ExpressionEvaluator(new Environment(), ports));
    }

    private static ReadInput readInput(String prompt) {
        return new ReadInput(new StringLiteral(prompt, RANGE), RANGE);
    }

    private static ReadEnv readEnv(String name) {
        return new ReadEnv(new StringLiteral(name, RANGE), RANGE);
    }

    @Test
    void readInputWritesThePromptWithoutABreakAndGivesBackTheLine() {
        Result<Object> result = evaluate(readInput("dame: "), ports(List.of("hola"), Map.of()));

        assertEquals("hola", assertInstanceOf(Success.class, result).value());
        assertEquals(List.of("dame: "), written);
    }

    @Test
    void readInputWithoutMoreInputFails() {
        Failure<?> failure =
                assertInstanceOf(
                        Failure.class, evaluate(readInput("dame: "), ports(List.of(), Map.of())));

        assertEquals("No hay más entrada disponible para readInput", failure.message());
        assertEquals(RANGE, failure.range());
    }

    @Test
    void aPromptThatCannotBeEvaluatedTravelsAsData() {
        Expression broken = new ReadInput(new Identifier("noExiste", RANGE), RANGE);

        assertInstanceOf(Failure.class, evaluate(broken, ports(List.of("x"), Map.of())));
    }

    @Test
    void readEnvGoesThroughThePort() {
        Result<Object> result =
                evaluate(readEnv("MI_VAR"), ports(List.of(), Map.of("MI_VAR", "valor")));

        assertEquals("valor", assertInstanceOf(Success.class, result).value());
    }

    @Test
    void readEnvOfSomethingUndefinedFails() {
        Failure<?> failure =
                assertInstanceOf(
                        Failure.class, evaluate(readEnv("NO_ESTA"), ports(List.of(), Map.of())));

        assertEquals("La variable de entorno NO_ESTA no está definida", failure.message());
    }

    @Test
    void aNameThatCannotBeEvaluatedTravelsAsData() {
        Expression broken = new ReadEnv(new Identifier("noExiste", RANGE), RANGE);

        assertInstanceOf(Failure.class, evaluate(broken, ports(List.of(), Map.of())));
    }

    // --- la conversión por tipo declarado ---

    private Result<Void> declare(String type, Expression initializer, RuntimePorts ports) {
        Statement declaration =
                new VariableDeclaration(
                        new Identifier("x", RANGE),
                        new TypeName(type, RANGE),
                        Optional.of(initializer),
                        RANGE);
        return declaration.accept(new Executor(new Environment(), ports));
    }

    @Test
    void aValueThatIsNotTextPassesThroughUntouched() {
        // para cualquier inicializador que no sea una lectura la conversión es un no-op
        assertInstanceOf(
                Success.class,
                declare("number", new NumberLiteral(5, RANGE), ports(List.of(), Map.of())));
    }

    @Test
    void textIsConvertedToTheDeclaredType() {
        assertInstanceOf(
                Success.class, declare("number", readInput("n: "), ports(List.of("42"), Map.of())));
        assertInstanceOf(
                Success.class,
                declare("boolean", readInput("b: "), ports(List.of("false"), Map.of())));
        assertInstanceOf(
                Success.class, declare("string", readInput("s: "), ports(List.of("x"), Map.of())));
    }

    @Test
    void aConversionThatFailsIsAFailureWithItsRange() {
        Failure<?> number =
                assertInstanceOf(
                        Failure.class,
                        declare("number", readInput("n: "), ports(List.of("abc"), Map.of())));
        Failure<?> bool =
                assertInstanceOf(
                        Failure.class,
                        declare("boolean", readInput("b: "), ports(List.of("Si"), Map.of())));

        assertEquals("'abc' no es un number", number.message());
        assertEquals("'Si' no es un boolean", bool.message());
        assertEquals(RANGE, number.range());
    }
}
