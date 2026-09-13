package org.printscript.linter.rules.format;

// snake case: arranca en minúscula y solo admite minúsculas, dígitos y guión bajo
public final class SnakeCaseChecker implements IdentifierFormatChecker {

    @Override
    public boolean isValid(String name) {
        return !name.isEmpty()
                && Character.isLowerCase(name.charAt(0))
                && name.chars().allMatch(SnakeCaseChecker::isSnakeCharacter);
    }

    private static boolean isSnakeCharacter(int character) {
        return character == '_'
                || (Character.isLetterOrDigit(character) && !Character.isUpperCase(character));
    }

    @Override
    public String expectedFormat() {
        return "snake case";
    }
}
