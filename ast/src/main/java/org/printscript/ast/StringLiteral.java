package org.printscript.ast;

import org.printscript.common.Range;

public record StringLiteral(String value, Range range) implements Expression {
}
