package org.printscript.formatter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.printscript.config.RulesConfiguration;

class FormatterConfigurationTest {

    private static FormatterConfiguration from(Map<String, Object> values) {
        return FormatterConfiguration.from(new RulesConfiguration(values));
    }

    @Test
    void anEmptyConfigurationTurnsEveryOptionalRuleOff() {
        FormatterConfiguration configuration = from(Map.of());

        assertFalse(configuration.spaceBeforeColon());
        assertFalse(configuration.spaceAfterColon());
        assertFalse(configuration.spaceAroundAssign());
        assertEquals(0, configuration.blankLinesBeforePrintln());
    }

    @Test
    void readsTheSpaceBeforeColonRule() {
        assertTrue(
                from(Map.of(FormatterConfiguration.SPACE_BEFORE_COLON_KEY, true))
                        .spaceBeforeColon());
    }

    @Test
    void readsTheSpaceAfterColonRule() {
        assertTrue(
                from(Map.of(FormatterConfiguration.SPACE_AFTER_COLON_KEY, true)).spaceAfterColon());
    }

    @Test
    void readsTheSpaceAroundAssignRule() {
        assertTrue(
                from(Map.of(FormatterConfiguration.SPACE_AROUND_ASSIGN_KEY, true))
                        .spaceAroundAssign());
    }

    @Test
    void readsTheBlankLinesBeforePrintlnRule() {
        assertEquals(
                2,
                from(Map.of(FormatterConfiguration.BLANK_LINES_BEFORE_PRINTLN_KEY, 2))
                        .blankLinesBeforePrintln());
    }

    @Test
    void moreBlankLinesBeforePrintlnThanAllowedIsRejected() {
        Map<String, Object> values =
                Map.of(FormatterConfiguration.BLANK_LINES_BEFORE_PRINTLN_KEY, 3);

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> from(values));

        assertTrue(thrown.getMessage().contains("3"));
    }

    @Test
    void aNegativeAmountOfBlankLinesBeforePrintlnIsRejected() {
        Map<String, Object> values =
                Map.of(FormatterConfiguration.BLANK_LINES_BEFORE_PRINTLN_KEY, -1);

        assertThrows(IllegalArgumentException.class, () -> from(values));
    }
}
