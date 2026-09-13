package org.printscript.linter.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.linter.rules.format.SnakeCaseChecker;

class SnakeCaseCheckerTest {

    private final SnakeCaseChecker checker = new SnakeCaseChecker();

    @Test
    void acceptsLowercaseWordsSeparatedByUnderscores() {
        assertTrue(checker.isValid("my_variable"));
        assertTrue(checker.isValid("variable"));
        assertTrue(checker.isValid("una_variable_larga"));
        assertTrue(checker.isValid("valor_2"));
    }

    @Test
    void rejectsAnyUppercase() {
        assertFalse(checker.isValid("myVariable"));
        assertFalse(checker.isValid("my_Variable"));
        assertFalse(checker.isValid("MY_VARIABLE"));
    }

    @Test
    void rejectsNamesThatDoNotStartWithALowercaseLetter() {
        assertFalse(checker.isValid("_variable"));
        assertFalse(checker.isValid("2variable"));
        assertFalse(checker.isValid(""));
    }

    @Test
    void namesItsFormatForTheViolationMessage() {
        assertEquals("snake case", checker.expectedFormat());
    }
}
