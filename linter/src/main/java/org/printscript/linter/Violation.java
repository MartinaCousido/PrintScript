package org.printscript.linter;

import org.printscript.common.Range;

public record Violation(String message, Range range) {}
