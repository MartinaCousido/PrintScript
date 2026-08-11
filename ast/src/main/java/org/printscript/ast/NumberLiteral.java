package org.printscript.ast;

import org.printscript.common.Range;

public record NumberLiteral(double value, Range range) implements Expression {
}
