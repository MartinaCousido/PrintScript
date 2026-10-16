package org.printscript.linter;

import org.printscript.common.Range;

// rule es la clave con la que se configura la regla incumplida
public record Violation(String rule, String message, Range range) {}
