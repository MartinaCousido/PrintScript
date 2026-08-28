package org.printscript.linter.rules.format;

public interface IdentifierFormatChecker {

    boolean isValid(String name);

    String expectedFormat();
}
