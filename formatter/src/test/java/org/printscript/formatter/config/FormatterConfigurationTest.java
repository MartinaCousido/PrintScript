package org.printscript.formatter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.printscript.config.RulesConfiguration;

class FormatterConfigurationTest {

    private static FormatterConfiguration from(Map<String, Object> values) {
        return FormatterConfiguration.from(new RulesConfiguration(values));
    }

    @Test
    void anEmptyConfigurationLeavesEveryRuleOff() {
        FormatterConfiguration configuration = from(Map.of());

        assertEquals(FormatterConfiguration.none(), configuration);
        assertTrue(configuration.spacingAroundEquals().isEmpty());
        assertTrue(configuration.lineBreaksAfterPrintln().isEmpty());
        assertTrue(configuration.indentInsideIf().isEmpty());
    }

    @Test
    void theTwoKeysAboutTheEqualsConfigureTheSameSpace() {
        assertEquals(
                Optional.of(true),
                from(Map.of(FormatterConfiguration.SPACING_AROUND_EQUALS_KEY, true))
                        .spacingAroundEquals());
        assertEquals(
                Optional.of(false),
                from(Map.of(FormatterConfiguration.NO_SPACING_AROUND_EQUALS_KEY, true))
                        .spacingAroundEquals());
    }

    @Test
    void askingForSpaceWinsOverAskingForNoneWhenBothArePresent() {
        FormatterConfiguration configuration =
                from(
                        Map.of(
                                FormatterConfiguration.SPACING_AROUND_EQUALS_KEY, true,
                                FormatterConfiguration.NO_SPACING_AROUND_EQUALS_KEY, true));

        assertEquals(Optional.of(true), configuration.spacingAroundEquals());
    }

    @Test
    void theTwoKeysAboutTheBraceConfigureTheSamePlacement() {
        assertEquals(
                Optional.of(true),
                from(Map.of(FormatterConfiguration.IF_BRACE_SAME_LINE_KEY, true))
                        .ifBraceOnSameLine());
        assertEquals(
                Optional.of(false),
                from(Map.of(FormatterConfiguration.IF_BRACE_BELOW_LINE_KEY, true))
                        .ifBraceOnSameLine());
    }

    @Test
    void readsTheBooleanRulesAndTheNumericOnes() {
        FormatterConfiguration configuration =
                from(
                        Map.of(
                                FormatterConfiguration.SPACING_BEFORE_COLON_KEY, true,
                                FormatterConfiguration.SPACING_AFTER_COLON_KEY, true,
                                FormatterConfiguration.SPACE_SURROUNDING_OPERATIONS_KEY, true,
                                FormatterConfiguration.SINGLE_SPACE_SEPARATION_KEY, true,
                                FormatterConfiguration.LINE_BREAK_AFTER_STATEMENT_KEY, true,
                                FormatterConfiguration.LINE_BREAKS_AFTER_PRINTLN_KEY, 2,
                                FormatterConfiguration.INDENT_INSIDE_IF_KEY, 4));

        assertTrue(configuration.spacingBeforeColon());
        assertTrue(configuration.spacingAfterColon());
        assertTrue(configuration.spaceSurroundingOperations());
        assertTrue(configuration.singleSpaceSeparation());
        assertTrue(configuration.lineBreakAfterStatement());
        assertEquals(Optional.of(2), configuration.lineBreaksAfterPrintln());
        assertEquals(Optional.of(4), configuration.indentInsideIf());
    }

    @Test
    void aFalseBooleanIsTheSameAsAnAbsentRule() {
        assertFalse(
                from(Map.of(FormatterConfiguration.SPACING_BEFORE_COLON_KEY, false))
                        .spacingBeforeColon());
        assertTrue(
                from(Map.of(FormatterConfiguration.IF_BRACE_SAME_LINE_KEY, false))
                        .ifBraceOnSameLine()
                        .isEmpty());
    }

    @Test
    void negativeCountsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> from(Map.of(FormatterConfiguration.LINE_BREAKS_AFTER_PRINTLN_KEY, -1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> from(Map.of(FormatterConfiguration.INDENT_INSIDE_IF_KEY, -2)));
    }
}
