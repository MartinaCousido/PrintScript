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

class BooleanEndToEndTest {

    private static final class RecordingPrinter implements OutputPrinter {

        private final List<String> lines = new ArrayList<>();

        @Override
        public void print(String line) {
            lines.add(line);
        }
    }

    private static List<String> execute(Version version, String... lines) {
        RecordingPrinter printer = new RecordingPrinter();
        new ExecutionRunner().run(List.of(lines).iterator(), printer, version);
        return printer.lines;
    }

    private static List<Failure<?>> validate(Version version, String... lines) {
        return new ValidationRunner().validate(List.of(lines).iterator(), version);
    }

    @Test
    void aBooleanVariableDeclaresValidatesAndPrints() {
        assertEquals(List.of(), validate(Version.V11, "let ok: boolean = true;", "println(ok);"));
        assertEquals(
                List.of("true", "false"),
                execute(
                        Version.V11,
                        "let ok: boolean = true;",
                        "let no: boolean = false;",
                        "println(ok);",
                        "println(no);"));
    }

    @Test
    void aBooleanLiteralCanBePrintedDirectly() {
        assertEquals(List.of("true"), execute(Version.V11, "println(true);"));
    }

    @Test
    void aBooleanDoesNotTakePartInArithmeticOrConcatenation() {
        assertEquals(1, validate(Version.V11, "let x: number = true + 1;").size());
        assertEquals(1, validate(Version.V11, "let x: string = \"n: \" + true;").size());
        assertEquals(1, validate(Version.V11, "let x: number = true - false;").size());
    }

    @Test
    void aBooleanCannotBeAssignedToAnotherType() {
        List<Failure<?>> failures = validate(Version.V11, "let x: number = true;");

        assertEquals(1, failures.size());
        assertEquals(
                "No se puede asignar boolean a una variable number", failures.getFirst().message());
    }

    @Test
    void everyErrorCarriesItsRange() {
        List<Failure<?>> failures = validate(Version.V11, "let x: number = true;");

        assertEquals(1, failures.getFirst().range().start().line());
        assertEquals(17, failures.getFirst().range().start().column());
        assertEquals(20, failures.getFirst().range().end().column());
    }

    @Test
    void version10HasNeitherTheTypeNorTheLiteral() {
        // 'boolean' no es un tipo y 'true' no es un literal: el programa no compila
        assertFalse(validate(Version.V10, "let ok: boolean = true;").isEmpty());
        assertFalse(validate(Version.V10, "println(true);").isEmpty());
    }

    @Test
    void theFormatterWritesBooleanLiterals() {
        StringWriter output = new StringWriter();

        new FormattingRunner()
                .format(
                        List.of("let ok:boolean=true;").iterator(),
                        new FormatterConfiguration(false, true, true, 0),
                        output,
                        Version.V11);

        assertTrue(output.toString().contains("true"));
        assertTrue(output.toString().contains("ok: boolean"));
    }
}
