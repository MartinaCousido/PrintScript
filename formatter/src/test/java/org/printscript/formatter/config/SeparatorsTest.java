package org.printscript.formatter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeparatorsTest {

    private static Separators withColon(boolean before, boolean after) {
        return new Separators(new FormatterConfiguration(before, after, false, 0));
    }

    private static Separators withAssign(boolean around) {
        return new Separators(new FormatterConfiguration(false, false, around, 0));
    }

    @Test
    void theColonIsTightWhenNeitherSideAsksForASpace() {
        assertEquals(":", withColon(false, false).colon());
    }

    @Test
    void theColonOnlyTakesASpaceBefore() {
        assertEquals(" :", withColon(true, false).colon());
    }

    @Test
    void theColonOnlyTakesASpaceAfter() {
        assertEquals(": ", withColon(false, true).colon());
    }

    @Test
    void theColonTakesASpaceOnBothSides() {
        assertEquals(" : ", withColon(true, true).colon());
    }

    @Test
    void theAssignIsTightWhenTheRuleIsOff() {
        assertEquals("=", withAssign(false).assign());
    }

    @Test
    void theAssignTakesASpaceOnBothSidesWhenTheRuleIsOn() {
        assertEquals(" = ", withAssign(true).assign());
    }
}
