package org.printscript.formatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Result;
import org.printscript.common.Success;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.lexer.LexerFactory;

// El formatter reescribe el fuente: lo que no pide ninguna regla activa tiene que salir
// exactamente como entró. Cada test prende una sola regla, que es como las usa la consigna.
class FormatterTest {

    // una regla por vez, sobre la configuración vacía
    private static final class Rules {

        private Optional<Boolean> theEqualsSpacing = Optional.empty();
        private boolean theBeforeColon;
        private boolean theAfterColon;
        private boolean theOperations;
        private boolean theSingleSpace;
        private boolean theBreakAfterStatement;
        private Optional<Integer> thePrintlnBreaks = Optional.empty();
        private Optional<Boolean> theBraceSameLine = Optional.empty();
        private Optional<Integer> theIndent = Optional.empty();

        private Rules equalsSpacing(boolean value) {
            theEqualsSpacing = Optional.of(value);
            return this;
        }

        private Rules afterColon() {
            theAfterColon = true;
            return this;
        }

        private Rules beforeColon() {
            theBeforeColon = true;
            return this;
        }

        private Rules operations() {
            theOperations = true;
            return this;
        }

        private Rules singleSpace() {
            theSingleSpace = true;
            return this;
        }

        private Rules breakAfterStatement() {
            theBreakAfterStatement = true;
            return this;
        }

        private Rules printlnBreaks(int value) {
            thePrintlnBreaks = Optional.of(value);
            return this;
        }

        private Rules braceSameLine(boolean value) {
            theBraceSameLine = Optional.of(value);
            return this;
        }

        private Rules indent(int value) {
            theIndent = Optional.of(value);
            return this;
        }

        private FormatterConfiguration build() {
            return new FormatterConfiguration(
                    theEqualsSpacing,
                    theBeforeColon,
                    theAfterColon,
                    theOperations,
                    theSingleSpace,
                    theBreakAfterStatement,
                    thePrintlnBreaks,
                    theBraceSameLine,
                    theIndent);
        }
    }

    private static String format(String source, Rules rules) {
        return format(source, rules.build());
    }

    private static String format(String source, FormatterConfiguration configuration) {
        Formatter formatter = formatterOver(source, configuration);
        StringBuilder out = new StringBuilder();
        while (formatter.hasNext()) {
            out.append(((Success<String>) formatter.next()).value());
        }
        return out.toString();
    }

    private static Formatter formatterOver(String source, FormatterConfiguration configuration) {
        return new Formatter(
                LexerFactory.create(List.of(source.split("\n", -1)).iterator(), Version.V11),
                configuration);
    }

    @Test
    void withoutAnyRuleTheSourceComesOutUntouched() {
        String source =
                "let something:string = \"cosa\";\nlet other  :  number=  2;\nprintln(other);";

        assertEquals(source, format(source, FormatterConfiguration.none()));
    }

    @Test
    void spacingAroundEqualsOnlyTouchesTheEquals() {
        String source = "let a: string= \"x\";\nlet b: string =\"y\";";

        assertEquals(
                "let a: string = \"x\";\nlet b: string = \"y\";",
                format(source, new Rules().equalsSpacing(true)));
    }

    @Test
    void noSpacingAroundEqualsIsTheOppositeRule() {
        String source = "let a: string = \"x\";\nlet b: string =\"y\";";

        assertEquals(
                "let a: string=\"x\";\nlet b: string=\"y\";",
                format(source, new Rules().equalsSpacing(false)));
    }

    // el caso que separa a este formatter de un pretty-printer: la regla mira el espacio de
    // después del ':' y el de antes queda como estaba, aunque quede inconsistente
    @Test
    void spacingAfterColonLeavesTheSpaceBeforeTheColonAlone() {
        String source = "let a:string = \"x\";\nlet b : string = \"y\";";

        assertEquals(
                "let a: string = \"x\";\nlet b : string = \"y\";",
                format(source, new Rules().afterColon()));
    }

    @Test
    void spacingBeforeColonLeavesTheSpaceAfterItAlone() {
        String source = "let a:string = \"x\";";

        assertEquals("let a :string = \"x\";", format(source, new Rules().beforeColon()));
    }

    @Test
    void spaceSurroundingOperationsDoesNotTouchTheEquals() {
        String source = "let a: number=1+2*3;";

        assertEquals("let a: number=1 + 2 * 3;", format(source, new Rules().operations()));
    }

    @Test
    void singleSpaceSeparationCollapsesEverythingButKeepsTheSemicolonAttached() {
        String source = "let a:      string=\"x\";\nprintln(a);";

        assertEquals(
                "let a : string = \"x\";\nprintln ( a );",
                format(source, new Rules().singleSpace()));
    }

    @Test
    void lineBreakAfterStatementOnlySplitsWhatSharesALine() {
        String source = "let a: number=1;let b: number=2;\nlet c: number=3;";

        assertEquals(
                "let a: number=1;\nlet b: number=2;\nlet c: number=3;",
                format(source, new Rules().breakAfterStatement()));
    }

    @Test
    void lineBreaksAfterPrintlnAppliesOnlyAfterAPrintln() {
        String source = "let a: number=1;\nprintln(a);\nprintln(a);";

        assertEquals(
                "let a: number=1;\nprintln(a);\n\n\nprintln(a);",
                format(source, new Rules().printlnBreaks(2)));
    }

    @Test
    void zeroLineBreaksAfterPrintlnCollapsesTheBlankLinesThatWereThere() {
        String source = "println(1);\n\n\n\nprintln(2);";

        assertEquals("println(1);\nprintln(2);", format(source, new Rules().printlnBreaks(0)));
    }

    @Test
    void theBraceOfAnIfGoesWhereTheRuleSaysAndTheBodyIsNotTouched() {
        String sameLine = "let a: boolean = true;\nif (a)\n{\n  println(a);\n}";
        String below = "let a: boolean = true;\nif (a) {\n  println(a);\n}";

        assertEquals(
                "let a: boolean = true;\nif (a) {\n  println(a);\n}",
                format(sameLine, new Rules().braceSameLine(true)));
        assertEquals(
                "let a: boolean = true;\nif (a)\n{\n  println(a);\n}",
                format(below, new Rules().braceSameLine(false)));
    }

    @Test
    void indentInsideIfReindentsEveryLevelAndAlignsTheClosingBrace() {
        String source = "if (a) {\n  if (a) {\n    println(a);\n  }\n}";

        assertEquals(
                "if (a) {\n    if (a) {\n        println(a);\n    }\n}",
                format(source, new Rules().indent(4)));
    }

    @Test
    void unknownSymbolsAreReportedAsFailuresInsteadOfBeingWritten() {
        Formatter formatter = formatterOver("let a: number = #;", FormatterConfiguration.none());

        boolean sawFailure = false;
        while (formatter.hasNext()) {
            Result<String> piece = formatter.next();
            sawFailure = sawFailure || piece instanceof Failure<String>;
        }

        assertTrue(sawFailure, "el símbolo desconocido tiene que llegar como Failure");
    }
}
