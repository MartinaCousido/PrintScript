package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Failure;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;

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
                        .format(
                                List.of(TYPE_ERROR).iterator(),
                                new FormatterConfiguration(false, true, true, 0),
                                output);

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
                                        Optional.of(IdentifierFormat.CAMEL_CASE), true));

        assertEquals(List.of(), report.failures());
        // el nombre en snake case sí se reporta: el linter hace su trabajo igual
        assertFalse(report.violations().isEmpty());
    }

    @Test
    void executionRejectsTheProgramBeforeRunningIt() {
        List<Failure<?>> failures =
                new ExecutionRunner().run(List.of(TYPE_ERROR).iterator(), line -> {});

        assertFalse(failures.isEmpty());
    }
}
