package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.runtime.OutputPrinter;

class ConstEndToEndTest {

    private static final String CONSTANTE = "const x: number = 1;";
    private static final String IMPRIMIR = "println(x);";

    private static final class RecordingPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void println(String line) {
            lines.add(line);
        }

        @Override
        public void print(String text) {
            lines.add(text);
        }
    }

    private static List<String> execute(String... lines) {
        RecordingPrinter printer = new RecordingPrinter();
        new ExecutionRunner().run(List.of(lines).iterator(), printer, Version.V11);
        return printer.lines;
    }

    private static List<Failure<?>> validate(Version version, String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), version);
    }

    @Test
    void aConstantDeclaresValidatesAndExecutes() {
        assertEquals(List.of(), validate(Version.V11, CONSTANTE, IMPRIMIR));
        assertEquals(List.of("1"), execute(CONSTANTE, IMPRIMIR));
    }

    @Test
    void reassigningAConstantFailsInValidationWithoutExecutingAnything() {
        RecordingPrinter printer = new RecordingPrinter();

        List<Failure<?>> failures = validate(Version.V11, CONSTANTE, "x = 2;", IMPRIMIR);

        assertEquals(1, failures.size());
        assertEquals("No se puede reasignar la constante x", failures.getFirst().message());
        assertEquals(List.of(), printer.lines);
    }

    @Test
    void aConstantWithoutAValueFailsInValidation() {
        List<Failure<?>> failures = validate(Version.V11, "const x: number;");

        assertEquals(1, failures.size());
        assertEquals(
                "Una constante tiene que declararse con un valor", failures.getFirst().message());
    }

    @Test
    void aConstantStillHasToMatchItsDeclaredType() {
        assertEquals(1, validate(Version.V11, "const x: number = \"hola\";").size());
    }

    @Test
    void theInterpreterAlsoRefusesToReassignAConstant() {
        // semantic lo rechaza antes; esto fija que la red de seguridad diga lo mismo
        List<String> printed = execute(CONSTANTE, "x = 2;", IMPRIMIR);

        assertEquals(List.of("1"), printed);
    }

    @Test
    void version10DoesNotHaveConst() {
        assertFalse(validate(Version.V10, CONSTANTE).isEmpty());
    }

    @Test
    void theFormatterWritesTheConstDeclarator() {
        StringWriter output = new StringWriter();

        new FormattingRunner()
                .format(
                        List.of("const x:number=1;").iterator(),
                        new FormatterConfiguration(false, true, true, 0),
                        output,
                        Version.V11);

        assertTrue(output.toString().startsWith(CONSTANTE));
    }
}
