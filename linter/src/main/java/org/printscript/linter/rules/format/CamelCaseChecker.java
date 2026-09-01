package org.printscript.linter.rules.format;

public final class CamelCaseChecker implements IdentifierFormatChecker {

    @Override
    public boolean isValid(String name) {
        return !name.isEmpty()
                && Character.isLowerCase(name.charAt(0))
                && name.chars().allMatch(Character::isLetterOrDigit);
    }

    @Override
    public String expectedFormat() {
        return "camel case";
    }
}
