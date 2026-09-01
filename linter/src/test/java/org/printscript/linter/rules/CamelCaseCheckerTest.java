package org.printscript.linter.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.printscript.linter.rules.format.CamelCaseChecker;

class CamelCaseCheckerTest {

    private final CamelCaseChecker checker = new CamelCaseChecker();

    @Test
    void aSingleLowercaseWordIsCamelCase() {
        assertTrue(checker.isValid("nombre"));
    }

    @Test
    void severalWordsJoinedWithCapitalsAreCamelCase() {
        assertTrue(checker.isValid("nombreDelUsuario"));
    }

    @Test
    void digitsAfterTheFirstLetterAreAllowed() {
        assertTrue(checker.isValid("linea2"));
    }

    @Test
    void startingWithACapitalIsNotCamelCase() {
        assertFalse(checker.isValid("NombreDelUsuario"));
    }

    @Test
    void anUnderscoreIsNotCamelCase() {
        assertFalse(checker.isValid("nombre_del_usuario"));
    }

    @Test
    void startingWithAnUnderscoreIsNotCamelCase() {
        assertFalse(checker.isValid("_nombre"));
    }

    @Test
    void startingWithADigitIsNotCamelCase() {
        assertFalse(checker.isValid("2linea"));
    }

    @Test
    void anEmptyNameIsNotCamelCase() {
        assertFalse(checker.isValid(""));
    }

    @Test
    void theCheckerNamesTheFormatItExpects() {
        assertTrue(checker.expectedFormat().contains("camel"), checker.expectedFormat());
    }
}
