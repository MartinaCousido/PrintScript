package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runtime.OutputPrinter;

// El formatter y el linter no pasan por semantic a propósito: se tiene que poder
// formatear y lintear código con errores de tipos, que es justo cuando más falta hace.
// Estos tests son los que hacen que esa decisión no se pierda si alguien "arregla" el
// runner encadenando semantic en las cuatro fachadas.
class ToolsToleratesTypeErrorsEndToEndTest {

    // un programa sintácticamente válido y semánticamente roto: string en un number
    private static final String[] TYPE_ERROR = {"let mal_nombre: number = \"hola\";"};

    @Test
    void validationRejectsTheProgram() {
        List<Failure<?>> failures = new ValidationRunner().validate(List.of(TYPE_ERROR).iterator());

        assertEquals(1, failures.size());
    }

    @Test
    void formattingStillWorksOnAProgramWithTypeErrors() {
        StringWriter output = new StringWriter();

        List<Failure<?>> failures =
                new FormattingRunner()
                        .format(List.of(TYPE_ERROR).iterator(), FormatterStyles.consigna(), output);

        assertEquals(List.of(), failures);
        assertFalse(output.toString().isEmpty());
    }

    @Test
    void lintingStillWorksOnAProgramWithTypeErrors() {
        AnalysisReport report =
                new AnalyzingRunner()
                        .analyze(
                                List.of(TYPE_ERROR).iterator(),
                                new LinterConfiguration(
                                        Optional.of(IdentifierFormat.CAMEL_CASE), true, false));

        assertEquals(List.of(), report.failures());
        // el nombre en snake case sí se reporta: el linter hace su trabajo igual
        assertFalse(report.violations().isEmpty());
    }

    @Test
    void executionRejectsTheProgramBeforeRunningIt() {
        List<Failure<?>> failures =
                new ExecutionRunner().run(List.of(TYPE_ERROR).iterator(), new Discarding());

        assertFalse(failures.isEmpty());
    }

    // no le interesa la salida, pero OutputPrinter ya no es funcional: son dos métodos
    private static final class Discarding implements OutputPrinter {

        @Override
        public void println(String line) {
            // descartado a propósito
        }

        @Override
        public void print(String text) {
            // descartado a propósito
        }
    }
}
