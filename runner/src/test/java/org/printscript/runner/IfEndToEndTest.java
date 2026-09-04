package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.runtime.OutputPrinter;

// Los casos de scope están escritos una sola vez y se corren contra las dos etapas a
// propósito: si semantic y el intérprete no abren y cierran el frame en el mismo lugar,
// el modo Validation y la ejecución dirían cosas distintas. Ese es el defecto central
// del repo relevado, que directamente no tiene scope de bloque.
class IfEndToEndTest {

    private static final class RecordingPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String line) {
            lines.add(line);
        }
    }

    private static List<String> execute(String... lines) {
        RecordingPrinter printer = new RecordingPrinter();
        new ExecutionRunner().run(List.of(lines).iterator(), printer, Version.V11);
        return printer.lines;
    }

    private static List<Failure<?>> validate(String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), Version.V11);
    }

    private static void assertValid(String... lines) {
        assertEquals(List.of(), validate(lines));
    }

    private static void assertRejected(String... lines) {
        assertFalse(validate(lines).isEmpty());
    }

    // --- ejecución ---

    @Test
    void aTrueConditionRunsTheThenBlock() {
        assertEquals(List.of("adentro"), execute("if (true) { println(\"adentro\"); }"));
    }

    @Test
    void aFalseConditionWithoutElseRunsNothing() {
        assertEquals(List.of(), execute("if (false) { println(\"adentro\"); }"));
    }

    @Test
    void exactlyOneBranchRuns() {
        assertEquals(
                List.of("else"),
                execute("if (false) { println(\"then\"); } else { println(\"else\"); }"));
    }

    @Test
    void anElseIfChainRunsTheFirstBranchWhoseConditionHolds() {
        String[] program = {
            "let a: boolean = false;",
            "let b: boolean = true;",
            "if (a) { println(\"primera\"); }",
            "else if (b) { println(\"segunda\"); }",
            "else { println(\"tercera\"); }"
        };

        assertValid(program);
        assertEquals(List.of("segunda"), execute(program));
    }

    @Test
    void aNestedIfRunsOnlyWhenBothConditionsHold() {
        assertEquals(
                List.of("las dos"), execute("if (true) { if (true) { println(\"las dos\"); } }"));
        assertEquals(List.of(), execute("if (true) { if (false) { println(\"no\"); } }"));
    }

    @Test
    void theStatementsAfterAnIfKeepRunning() {
        assertEquals(
                List.of("adentro", "despues"),
                execute("if (true) { println(\"adentro\"); }", "println(\"despues\");"));
    }

    // --- scope: los mismos casos en validación y en ejecución ---

    @Test
    void aVariableDeclaredInsideABlockDoesNotEscape() {
        assertRejected("if (true) { let x: number = 1; }", "println(x);");
        assertEquals(List.of(), execute("if (true) { let x: number = 1; }", "println(x);"));
    }

    @Test
    void theSameNameCanBeDeclaredInThenAndInElse() {
        String[] program = {
            "if (true) { let y: number = 1; println(y); }",
            "else { let y: number = 2; println(y); }"
        };

        assertValid(program);
        assertEquals(List.of("1"), execute(program));
    }

    @Test
    void anInnerDeclarationShadowsTheOuterOneAndDoesNotOutliveTheBlock() {
        String[] program = {
            "let z: number = 1;", "if (true) { let z: number = 2; println(z); }", "println(z);"
        };

        assertValid(program);
        assertEquals(List.of("2", "1"), execute(program));
    }

    @Test
    void anOuterVariableCanBeAssignedFromInsideABlock() {
        String[] program = {"let n: number = 1;", "if (true) { n = 2; }", "println(n);"};

        assertValid(program);
        assertEquals(List.of("2"), execute(program));
    }

    @Test
    void declaringTheSameNameTwiceInTheSameBlockIsStillAnError() {
        assertRejected("if (true) { let x: number = 1; let x: number = 2; }");
    }

    // --- la condición ---

    @Test
    void aConditionThatIsNotBooleanIsRejectedWithoutExecutingAnything() {
        List<Failure<?>> failures = validate("if (5) { println(\"no\"); }");

        assertEquals(1, failures.size());
        assertEquals("La condición de un if tiene que ser boolean", failures.getFirst().message());
        assertEquals(List.of(), execute("if (5) { println(\"no\"); }"));
    }

    @Test
    void aConditionCanBeAVariableOrAReadOfOne() {
        assertValid("let flag: boolean = true;", "if (flag) { println(\"si\"); }");
        assertEquals(
                List.of("si"),
                execute("let flag: boolean = true;", "if (flag) { println(\"si\"); }"));
    }

    @Test
    void anUndeclaredNameInTheConditionIsReported() {
        assertRejected("if (flag) { println(\"no\"); }");
    }

    // --- constantes adentro de un bloque ---

    @Test
    void aConstantDeclaredInsideABlockKeepsItsRule() {
        assertRejected("if (true) { const c: number = 1; c = 2; }");
    }

    // --- la compuerta de versión ---

    @Test
    void version10DoesNotHaveTheIfStatement() {
        assertFalse(
                new ValidationRunner()
                        .validate(List.of("if (true) { println(\"no\"); }").iterator(), Version.V10)
                        .isEmpty());
    }
}
