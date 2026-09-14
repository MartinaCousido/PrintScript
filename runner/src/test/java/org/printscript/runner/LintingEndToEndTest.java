package org.printscript.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.common.Position;
import org.printscript.common.Range;
import org.printscript.linter.Violation;
import org.printscript.linter.config.IdentifierFormat;
import org.printscript.linter.config.LinterConfiguration;

class LintingEndToEndTest {

    private static final LinterConfiguration TODO_PRENDIDO =
            new LinterConfiguration(Optional.of(IdentifierFormat.CAMEL_CASE), true, false);

    private static AnalysisReport analyze(LinterConfiguration configuration, String... lines) {
        return new AnalyzingRunner().analyze(List.of(lines).iterator(), configuration);
    }

    private static InputStream jsonOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void theExamplesOfTheConsignaHaveNoViolations() {
        AnalysisReport report =
                analyze(
                        TODO_PRENDIDO,
                        "let name: string = \"Joe\";",
                        "let lastName: string = \"Doe\";",
                        "println(lastName);");

        assertEquals(List.of(), report.violations());
        assertEquals(List.of(), report.failures());
    }

    @Test
    void aDeclarationWithASnakeCaseNameIsReportedAtItsExactPosition() {
        AnalysisReport report = analyze(TODO_PRENDIDO, "let last_name: string = \"Doe\";");

        assertEquals(1, report.violations().size());
        Violation violation = report.violations().get(0);
        assertTrue(violation.message().contains("last_name"), violation.message());
        assertEquals(new Range(new Position(1, 5), new Position(1, 13)), violation.range());
    }

    @Test
    void aPrintlnWithAnExpressionIsReportedAtThePositionOfTheArgument() {
        AnalysisReport report = analyze(TODO_PRENDIDO, "println(1 + 2);");

        assertEquals(1, report.violations().size());
        assertEquals(
                new Range(new Position(1, 9), new Position(1, 13)),
                report.violations().get(0).range());
    }

    @Test
    void everyViolationOfEveryLineIsReportedAndNoneStopsTheRest() {
        AnalysisReport report =
                analyze(
                        TODO_PRENDIDO,
                        "let user_name: string = \"Joe\";",
                        "let user_age: number = 20;",
                        "println(user_name + user_age);");

        assertEquals(5, report.violations().size());
    }

    @Test
    void aSyntaxErrorIsReportedApartFromTheViolationsAndDoesNotStopTheAnalysis() {
        AnalysisReport report =
                analyze(
                        TODO_PRENDIDO,
                        "let a: number = 1",
                        "let b: number = 2;",
                        "let bad_name: number = 3;");

        assertEquals(1, report.failures().size());
        assertEquals(1, report.violations().size());
        assertTrue(
                report.violations().get(0).message().contains("bad_name"),
                report.violations().get(0).message());
    }

    @Test
    void withTheRulesOffTheSameProgramIsClean() {
        LinterConfiguration apagado = new LinterConfiguration(Optional.empty(), false, false);

        AnalysisReport report =
                analyze(apagado, "let user_name: string = \"Joe\";", "println(1 + 2);");

        assertEquals(List.of(), report.violations());
    }

    @Test
    void theConfigurationCanComeFromAJsonFile() {
        AnalysisReport report =
                new AnalyzingRunner()
                        .analyze(
                                List.of("let user_name: number = 1;").iterator(),
                                jsonOf(
                                        "{\"identifier_format\": \"camel case\","
                                                + " \"mandatory-variable-or-literal-in-println\":"
                                                + " true}"),
                                "json");

        assertEquals(1, report.violations().size());
    }

    @Test
    void theViolationsOfALaterLineCarryTheirRealLineNumber() {
        AnalysisReport report =
                analyze(TODO_PRENDIDO, "let ok: number = 1;", "let not_ok: number = 2;");

        assertEquals(2, report.violations().get(0).range().start().line());
    }
}
