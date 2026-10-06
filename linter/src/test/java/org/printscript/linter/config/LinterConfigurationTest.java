package org.printscript.linter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.config.RulesConfiguration;

class LinterConfigurationTest {

    private static final String FORMAT_KEY = "identifier_format";
    private static final String PRINTLN_KEY = "mandatory-variable-or-literal-in-println";
    private static final String CAMEL = "camel case";

    private static LinterConfiguration from(Map<String, Object> values) {
        return LinterConfiguration.from(new RulesConfiguration(values));
    }

    @Test
    void anEmptyFileLeavesEveryRuleOff() {
        LinterConfiguration configuration = from(Map.of());

        assertTrue(configuration.identifierFormat().isEmpty());
        assertFalse(configuration.printlnOnlySimpleArgument());
    }

    @Test
    void theIdentifierFormatIsReadFromTheFile() {
        LinterConfiguration configuration = from(Map.of(FORMAT_KEY, CAMEL));

        assertEquals(Optional.of(IdentifierFormat.CAMEL_CASE), configuration.identifierFormat());
    }

    @Test
    void theIdentifierFormatIsAcceptedInItsUsualSpellings() {
        assertEquals(
                Optional.of(IdentifierFormat.CAMEL_CASE),
                from(Map.of(FORMAT_KEY, "camelCase")).identifierFormat());
        assertEquals(
                Optional.of(IdentifierFormat.CAMEL_CASE),
                from(Map.of(FORMAT_KEY, "camel_case")).identifierFormat());
        assertEquals(
                Optional.of(IdentifierFormat.CAMEL_CASE),
                from(Map.of(FORMAT_KEY, "CAMEL CASE")).identifierFormat());
    }

    @Test
    void thePrintlnRuleIsReadFromTheFile() {
        assertTrue(from(Map.of(PRINTLN_KEY, true)).printlnOnlySimpleArgument());
        assertFalse(from(Map.of(PRINTLN_KEY, false)).printlnOnlySimpleArgument());
    }

    @Test
    void anUnknownIdentifierFormatFailsInsteadOfTurningTheRuleOffInSilence() {
        Map<String, Object> values = Map.of(FORMAT_KEY, "kebab case");

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> from(values));

        assertTrue(thrown.getMessage().contains("kebab case"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(CAMEL), thrown.getMessage());
    }

    // el TCK trae casos con las dos convenciones, así que las dos tienen que resolverse
    @Test
    void snakeCaseIsSupportedInItsThreeSpellings() {
        for (String spelling : new String[] {"snake case", "snakecase", "snake_case"}) {
            assertEquals(
                    Optional.of(IdentifierFormat.SNAKE_CASE),
                    from(Map.of(FORMAT_KEY, spelling)).identifierFormat(),
                    spelling);
        }
    }

    @Test
    void theReadInputRuleIsOffUnlessItsKeyIsPresent() {
        assertFalse(from(Map.of()).readInputOnlySimpleArgument());
        assertTrue(
                from(Map.of("mandatory-variable-or-literal-in-readInput", true))
                        .readInputOnlySimpleArgument());
    }

    @Test
    void wrongRuleTypesFailInsteadOfDisablingTheRule() {
        assertThrows(IllegalArgumentException.class, () -> from(Map.of(FORMAT_KEY, true)));
    }
}
