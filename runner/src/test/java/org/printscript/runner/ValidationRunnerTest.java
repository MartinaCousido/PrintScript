package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Position;
import org.printscript.common.Range;

class ValidationRunnerTest {

    private static List<Failure<?>> validateProgram(String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator());
    }

    // originalOut es un alias de System.out para restaurarlo en el finally, no un
    // recurso propio que haya que cerrar.
    @Test
    @SuppressWarnings("PMD.CloseResource")
    void validProgramWithPrintlnProducesNoFailuresAndNoOutput() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            List<Failure<?>> failures = validateProgram("let x: number = 1 + 2;", "println(x);");

            assertEquals(List.of(), failures);
            assertEquals("", captured.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void emptyProgramProducesNoFailures() {
        List<Failure<?>> failures = validateProgram();

        assertEquals(List.of(), failures);
    }

    @Test
    void reportsMissingSemicolonWithMessageAndRange() {
        List<Failure<?>> failures = validateProgram("let x: number = 1");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertTrue(failure.message().contains(";"));
        assertEquals(new Range(new Position(1, 17), new Position(1, 17)), failure.range());
    }

    @Test
    void reportsUnrecognizedStatementWithFullRangeOnItsLine() {
        List<Failure<?>> failures = validateProgram("let x: number = 1;", "42;");

        assertEquals(1, failures.size());
        Failure<?> failure = failures.getFirst();
        assertEquals("Unrecognized statement", failure.message());
        assertEquals(new Position(2, 1), failure.range().start());
        assertEquals(new Position(2, 2), failure.range().end());
    }

    // --- semántica: lo que antes solo se descubría ejecutando ---

    @Test
    void reportsATypeMismatchWithoutExecutingAnything() {
        List<Failure<?>> failures = validateProgram("let x: number = \"hola\";");

        assertEquals(1, failures.size());
        assertEquals(
                "No se puede asignar string a una variable number", failures.getFirst().message());
    }

    @Test
    void reportsAnUndeclaredVariable() {
        List<Failure<?>> failures = validateProgram("println(x);");

        assertEquals(1, failures.size());
        assertEquals("Variable no declarada: x", failures.getFirst().message());
    }

    @Test
    void reportsARedeclaration() {
        List<Failure<?>> failures = validateProgram("let x: number = 1;", "let x: number = 2;");

        assertEquals(1, failures.size());
        assertEquals("Variable ya declarada: x", failures.getFirst().message());
    }

    @Test
    void reportsAnUnknownTypeAnnotation() {
        List<Failure<?>> failures = validateProgram("let x: boolean = 1;");

        assertEquals(1, failures.size());
        assertEquals("Tipo desconocido: boolean", failures.getFirst().message());
    }

    @Test
    void collectsEveryErrorOfTheProgramWithoutOneCondemningTheNext() {
        List<Failure<?>> failures =
                validateProgram(
                        "let a: number = \"uno\";", "println(a);", "let b: number = \"dos\";");

        // los dos errores de tipo, y el println del medio no falla: 'a' quedó declarada
        assertEquals(2, failures.size());
    }

    @Test
    void theConsignaExamplesValidateWithoutErrors() {
        assertEquals(
                List.of(),
                validateProgram(
                        "let name: string = \"Joe\";",
                        "let lastName: string = \"Doe\";",
                        "println(name + \" \" + lastName);"));
        assertEquals(
                List.of(),
                validateProgram(
                        "let a: number = 12;",
                        "let b: number = 4;",
                        "let c: number = a / b;",
                        "println(\"Result: \" + c);"));
        assertEquals(
                List.of(),
                validateProgram(
                        "let a: number = 12;",
                        "let b: number = 4;",
                        "a = a / b;",
                        "println(\"Result: \" + a);"));
    }

    @Test
    void collectsEverySyntaxErrorOfTheProgram() {
        List<Failure<?>> failures = validateProgram("42;", "println(\"ok\");", "7;");

        assertEquals(2, failures.size());
    }
}
