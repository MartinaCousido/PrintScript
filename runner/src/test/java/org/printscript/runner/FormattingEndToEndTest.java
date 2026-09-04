package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.runtime.OutputPrinter;

class FormattingEndToEndTest {

    private static final class RecordingPrinter implements OutputPrinter {

        private final List<String> collected = new ArrayList<>();

        @Override
        public void println(String line) {
            collected.add(line);
        }

        @Override
        public void print(String text) {
            collected.add(text);
        }

        private List<String> printed() {
            return collected;
        }
    }

    private static final FormatterConfiguration ESTILO_DE_LA_CONSIGNA =
            new FormatterConfiguration(false, true, true, 0);

    private static final String DECLARACION_APRETADA = "let a:number=12;";

    private static String format(FormatterConfiguration configuration, String... lines) {
        StringWriter output = new StringWriter();
        List<Failure<?>> failures =
                new FormattingRunner().format(List.of(lines).iterator(), configuration, output);
        assertEquals(List.of(), failures);
        return output.toString();
    }

    private static List<String> execute(String... lines) {
        RecordingPrinter printer = new RecordingPrinter();
        new ExecutionRunner().run(List.of(lines).iterator(), printer);
        return printer.printed();
    }

    private static String[] linesOf(String program) {
        return program.split("\n", -1);
    }

    @Test
    void theExampleOfTheConsignaComesOutInTheConfiguredStyle() {
        String output =
                format(
                        ESTILO_DE_LA_CONSIGNA,
                        DECLARACION_APRETADA,
                        "let b   :   number    =4;",
                        "a=a/b;",
                        "println(\"Result: \"+a);");

        assertEquals(
                """
                let a: number = 12;
                let b: number = 4;
                a = a / b;
                println("Result: " + a);
                """,
                output);
    }

    @Test
    void severalStatementsCrammedIntoOneLineComeOutOnePerLine() {
        String output = format(ESTILO_DE_LA_CONSIGNA, "let a: number = 1; let b: number = 2;");

        assertEquals("let a: number = 1;\nlet b: number = 2;\n", output);
    }

    @Test
    void formattingAnAlreadyFormattedProgramChangesNothing() {
        String once = format(ESTILO_DE_LA_CONSIGNA, DECLARACION_APRETADA, "println(a);");
        String twice = format(ESTILO_DE_LA_CONSIGNA, linesOf(once));

        assertEquals(once, twice);
    }

    @Test
    void theBlankLinesBeforePrintlnRuleIsApplied() {
        String output =
                format(
                        new FormatterConfiguration(false, true, true, 1),
                        "let a: number = 1;",
                        "println(a);");

        assertEquals("let a: number = 1;\n\nprintln(a);\n", output);
    }

    @Test
    void theConfigurationCanComeFromAJsonFile() throws IOException {
        StringWriter output = new StringWriter();
        String json =
                """
                {
                  "enforce-spacing-after-colon-in-declaration": true,
                  "enforce-spacing-around-equals": true
                }
                """;

        try (InputStream configuration =
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            new FormattingRunner()
                    .format(
                            List.of(DECLARACION_APRETADA).iterator(),
                            configuration,
                            "json",
                            output);
        }

        assertEquals("let a: number = 12;\n", output.toString());
    }

    @Test
    void aSyntaxErrorIsReportedAndNotFormattedAway() {
        StringWriter output = new StringWriter();

        List<Failure<?>> failures =
                new FormattingRunner()
                        .format(
                                List.of("let a: number = ;").iterator(),
                                ESTILO_DE_LA_CONSIGNA,
                                output);

        assertFalse(failures.isEmpty(), "se esperaba al menos un error de sintaxis");
    }

    @Test
    void theFormattedProgramStillPrintsTheSameThing() {
        String formatted =
                format(ESTILO_DE_LA_CONSIGNA, DECLARACION_APRETADA, "println(\"a: \"+a);");

        List<String> original = execute(DECLARACION_APRETADA, "println(\"a: \"+a);");

        assertEquals(List.of("a: 12"), original);
        assertEquals(original, execute(linesOf(formatted)));
    }

    @Test
    void formattingDoesNotChangeTheResultOfAnExpressionThatNeedsParentheses() {
        String formatted = format(ESTILO_DE_LA_CONSIGNA, "let a:number=(1+2)*3;", "println(a);");

        assertEquals(List.of("9"), execute(linesOf(formatted)));
    }
}
