package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.linter.Violation;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;

// el formatter y el linter sobre las construcciones de 1.1
class IfToolsEndToEndTest {

    private static final FormatterConfiguration LLAVE_AL_LADO =
            FormatterStyles.consignaConBloques(4, true);
    private static final FormatterConfiguration LLAVE_ABAJO =
            FormatterStyles.consignaConBloques(2, false);

    private static String format(FormatterConfiguration configuration, String... lines) {
        StringWriter output = new StringWriter();
        new FormattingRunner()
                .format(List.of(lines).iterator(), configuration, output, Version.V11);
        return output.toString();
    }

    private static String[] linesOf(String program) {
        return program.split("\n", -1);
    }

    private static List<Violation> lint(String... lines) {
        return new AnalyzingRunner()
                .analyze(
                        List.of(lines).iterator(),
                        new LinterConfiguration(
                                Optional.of(IdentifierFormat.CAMEL_CASE), true, false),
                        Version.V11)
                .violations();
    }

    // --- formatter ---
    //
    // El formatter reescribe el fuente: toca el espacio que alguna regla nombra y deja el
    // resto como estaba. Por eso los programas de entrada acá vienen con los saltos de
    // línea que va a tener la salida: poner un if entero en una línea y esperar que quede
    // desplegado sería pedirle que imprima el árbol, que es lo que dejó de hacer.

    @Test
    void aBlockIsIndentedWithTheConfiguredSize() {
        String programa = "if (true) {\n  println(1);\n}";

        assertEquals("if (true) {\n    println(1);\n}", format(LLAVE_AL_LADO, linesOf(programa)));
    }

    @Test
    void nestingAccumulatesTheIndentation() {
        String programa = "if (true) {\n if (false) {\n  println(1);\n }\n}";

        assertEquals(
                "if (true) {\n    if (false) {\n        println(1);\n    }\n}",
                format(LLAVE_AL_LADO, linesOf(programa)));
    }

    @Test
    void theBraceGoesWhereTheRuleSaysWithoutTouchingTheBody() {
        String conLlaveAlLado = "if (true) {\n  println(1);\n}";

        assertEquals(
                "if (true)\n{\n  println(1);\n}", format(LLAVE_ABAJO, linesOf(conLlaveAlLado)));
    }

    @Test
    void theStatementsInsideABlockKeepTheirOwnFormattingRules() {
        String programa = "if (true) {\n  let x:number=1;\n}";

        assertEquals(
                "if (true) {\n    let x: number = 1;\n}", format(LLAVE_AL_LADO, linesOf(programa)));
    }

    // sin ninguna regla de bloques activa, un if entero en una línea sale igual que entró:
    // ninguna regla habla de dónde van los saltos adentro de las llaves
    @Test
    void aOneLineIfIsLeftAloneWhenNoBlockRuleIsOn() {
        String programa = "if(true){println(1);}";

        assertEquals(programa, format(FormatterStyles.soloElEspacioDelIgual(), linesOf(programa)));
    }

    // --- linter ---

    @Test
    void anIdentifierInsideABlockIsReported() {
        assertEquals(1, lint("if (true) { let mal_nombre: number = 1; }").size());
    }

    @Test
    void anIdentifierInTheConditionIsReported() {
        // sin declararlo a propósito: el linter no pasa por semantic, así que puede
        // trabajar sobre código que no tiene sentido, y acá eso aísla la condición
        assertEquals(1, lint("if (mal_nombre) { println(1); }").size());
    }

    @Test
    void aPrintlnWithAnExpressionInsideABlockIsReported() {
        // sin recorrer el bloque, esta regla se saltea todos los println de adentro
        assertEquals(1, lint("if (true) { println(1 + 2); }").size());
    }

    @Test
    void everyViolationIsReportedAndNotJustTheFirst() {
        List<Violation> violations =
                lint(
                        "if (true) {",
                        "  let mal_uno: number = 1;",
                        "  let mal_dos: number = 2;",
                        "  println(1 + 2);",
                        "}");

        assertEquals(3, violations.size());
    }

    @Test
    void violationsFromBothBranchesAreReported() {
        assertEquals(
                2,
                lint("if (true) { let mal_uno: number = 1; }", "else { let mal_dos: number = 2; }")
                        .size());
    }
}
