package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
            new FormatterConfiguration(false, true, true, 0, 4, true);
    private static final FormatterConfiguration LLAVE_ABAJO =
            new FormatterConfiguration(false, true, true, 0, 2, false);

    private static String format(FormatterConfiguration configuration, String... lines) {
        StringWriter output = new StringWriter();
        new FormattingRunner()
                .format(List.of(lines).iterator(), configuration, output, Version.V11);
        return output.toString();
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

    @Test
    void aBlockIsIndentedWithTheConfiguredSize() {
        assertEquals(
                "if (true) {\n    println(1);\n}\n",
                format(LLAVE_AL_LADO, "if(true){println(1);}"));
        assertEquals(
                "if (true)\n{\n  println(1);\n}\n", format(LLAVE_ABAJO, "if(true){println(1);}"));
    }

    @Test
    void nestingAccumulatesTheIndentation() {
        assertEquals(
                "if (true) {\n    if (false) {\n        println(1);\n    }\n}\n",
                format(LLAVE_AL_LADO, "if(true){if(false){println(1);}}"));
    }

    @Test
    void anEmptyBlockKeepsItsBraces() {
        assertEquals("if (true) {\n}\n", format(LLAVE_AL_LADO, "if(true){}"));
    }

    @Test
    void anElseIfIsPrintedOnOneLineAndAnElseWithABlockIsNested() {
        // los dos programas hacen lo mismo pero son árboles distintos, y por eso se
        // pueden imprimir distinto: es la razón por la que Block es un nodo
        String elseIf = format(LLAVE_AL_LADO, "if(true){println(1);}else if(false){println(2);}");
        String elseConBloque =
                format(LLAVE_AL_LADO, "if(true){println(1);}else{if(false){println(2);}}");

        assertEquals(
                "if (true) {\n    println(1);\n} else if (false) {\n    println(2);\n}\n", elseIf);
        assertEquals(
                "if (true) {\n    println(1);\n} else {\n    if (false) {\n        println(2);\n    }\n}\n",
                elseConBloque);
        assertNotEquals(elseIf, elseConBloque);
    }

    @Test
    void theStatementsInsideABlockKeepTheirOwnFormattingRules() {
        assertEquals(
                "if (true) {\n    let x: number = 1;\n}\n",
                format(LLAVE_AL_LADO, "if(true){let x:number=1;}"));
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
