package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.common.Version;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runtime.OutputPrinter;

// El despacho por versión está instalado pero 1.1 todavía no agrega nada al lenguaje:
// mientras eso sea así, las dos versiones tienen que comportarse igual. Cuando 1.1
// empiece a sumar construcciones, estos tests van a marcar exactamente qué cambió.
class VersionDispatchEndToEndTest {

    private static final String[] PROGRAMA = {
        "let a: number = 12;", "let b: number = 4;", "println(\"Result: \" + a / b);"
    };

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

    private static List<String> execute(Version version) {
        RecordingPrinter printer = new RecordingPrinter();
        assertEquals(
                List.of(),
                new ExecutionRunner().run(List.of(PROGRAMA).iterator(), printer, version));
        return printer.lines;
    }

    private static String format(Version version) {
        StringWriter output = new StringWriter();
        new FormattingRunner()
                .format(
                        List.of(PROGRAMA).iterator(),
                        new FormatterConfiguration(false, true, true, 0),
                        output,
                        version);
        return output.toString();
    }

    @Test
    void executionGivesTheSameOutputInBothVersions() {
        assertEquals(List.of("Result: 3"), execute(Version.V10));
        assertEquals(execute(Version.V10), execute(Version.V11));
    }

    @Test
    void validationGivesTheSameResultInBothVersions() {
        List<Failure<?>> v10 =
                new ValidationRunner().validate(List.of(PROGRAMA).iterator(), Version.V10);
        List<Failure<?>> v11 =
                new ValidationRunner().validate(List.of(PROGRAMA).iterator(), Version.V11);

        assertEquals(List.of(), v10);
        assertEquals(v10, v11);
    }

    @Test
    void formattingGivesTheSameOutputInBothVersions() {
        assertEquals(format(Version.V10), format(Version.V11));
    }

    @Test
    void analyzingGivesTheSameReportInBothVersions() {
        LinterConfiguration configuration =
                new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), true);

        AnalysisReport v10 =
                new AnalyzingRunner()
                        .analyze(List.of(PROGRAMA).iterator(), configuration, Version.V10);
        AnalysisReport v11 =
                new AnalyzingRunner()
                        .analyze(List.of(PROGRAMA).iterator(), configuration, Version.V11);

        assertEquals(v10.violations(), v11.violations());
        assertEquals(v10.failures(), v11.failures());
    }

    @Test
    void omittingTheVersionMeansTheOneTheConsignaDefinesAsDefault() {
        RecordingPrinter porDefecto = new RecordingPrinter();
        RecordingPrinter explicita = new RecordingPrinter();

        new ExecutionRunner().run(List.of(PROGRAMA).iterator(), porDefecto);
        new ExecutionRunner().run(List.of(PROGRAMA).iterator(), explicita, Version.V10);

        assertEquals(explicita.lines, porDefecto.lines);
    }
}
